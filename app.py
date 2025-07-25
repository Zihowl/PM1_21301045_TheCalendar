import os
from dotenv import load_dotenv
import graphene
from flask import Flask, jsonify, request, send_from_directory
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from graphene_sqlalchemy import SQLAlchemyObjectType
from sqlalchemy import func, inspect, text
from werkzeug.security import generate_password_hash, check_password_hash
from werkzeug.utils import secure_filename
import jwt
from functools import wraps
from datetime import datetime, timedelta
from graphql_relay import from_global_id

# --- 1. CONFIGURACIÓN INICIAL ---

load_dotenv()
app = Flask(__name__)
CORS(app)
app.config['SECRET_KEY'] = os.getenv('SECRET_KEY')
app.config['SQLALCHEMY_DATABASE_URI'] = os.getenv('DATABASE_URI')
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db = SQLAlchemy(app)


# --- 2. MODELOS DE LA BASE DE DATOS ---
class Usuario(db.Model):
    __tablename__ = 'usuarios'
    id = db.Column(db.Integer, primary_key=True)
    nombre_usuario = db.Column(db.String(50), unique=True, nullable=False)
    contrasena_hash = db.Column(db.String(255), nullable=False)
    fecha_registro = db.Column(db.DateTime, default=datetime.now)
    updated_at = db.Column(db.DateTime, default=datetime.now, onupdate=datetime.now)
    materias = db.relationship('Materia', backref='usuario', cascade="all, delete-orphan")

    def __init__(self, n, c): self.nombre_usuario = n; self.contrasena_hash = generate_password_hash(c)

    def verificar_contrasena(self, c): return check_password_hash(self.contrasena_hash, c)


class Materia(db.Model):
    __tablename__ = 'materias'
    id = db.Column(db.Integer, primary_key=True)
    id_usuario = db.Column(db.Integer, db.ForeignKey('usuarios.id'), nullable=False)
    nombre = db.Column(db.String(100), nullable=False)
    profesor = db.Column(db.String(100))
    created_at = db.Column(db.DateTime, default=datetime.now)
    updated_at = db.Column(db.DateTime, default=datetime.now, onupdate=datetime.now)
    tareas = db.relationship('Tarea', backref='materia', lazy='subquery')
    notas = db.relationship('Nota', backref='materia', lazy='subquery')
    horarios = db.relationship(
        'Horario', backref='materia', cascade="all, delete-orphan", lazy='subquery'
    )


class Tarea(db.Model):
    __tablename__ = 'tareas'
    id = db.Column(db.Integer, primary_key=True)
    id_materia = db.Column(db.Integer, db.ForeignKey('materias.id'))
    id_usuario = db.Column(db.Integer, db.ForeignKey('usuarios.id'), nullable=False)
    titulo = db.Column(db.String(255), nullable=False)
    descripcion = db.Column(db.Text)
    fecha_entrega = db.Column(db.DateTime)
    completada = db.Column(db.Boolean, default=False)
    created_at = db.Column(db.DateTime, default=datetime.now)
    updated_at = db.Column(db.DateTime, default=datetime.now, onupdate=datetime.now)


class Nota(db.Model):
    __tablename__ = 'notas'
    id = db.Column(db.Integer, primary_key=True)
    id_materia = db.Column(db.Integer, db.ForeignKey('materias.id'))
    id_usuario = db.Column(db.Integer, db.ForeignKey('usuarios.id'), nullable=False)
    titulo = db.Column(db.String(255), nullable=False)
    contenido = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.now)
    updated_at = db.Column(db.DateTime, default=datetime.now, onupdate=datetime.now)


class Horario(db.Model):
    __tablename__ = 'horarios'
    id = db.Column(db.Integer, primary_key=True)
    id_materia = db.Column(db.Integer, db.ForeignKey('materias.id'), nullable=False)
    dia_semana = db.Column(db.Integer, nullable=False)
    hora_inicio = db.Column(db.Time, nullable=False)
    hora_fin = db.Column(db.Time, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.now)
    updated_at = db.Column(db.DateTime, default=datetime.now, onupdate=datetime.now)


class Imagen(db.Model):
    __tablename__ = 'imagenes'
    id = db.Column(db.Integer, primary_key=True)
    ruta = db.Column(db.String(255), nullable=False)
    fecha = db.Column(db.DateTime, default=datetime.utcnow)
# --- 3. DECORADOR DE TOKEN Y UTILIDADES ---
def token_required(f):
    @wraps(f)
    def decorated(root, info, **kwargs):
        token = info.context.headers.get('x-access-tokens')
        if not token: raise Exception('Falta el token de autenticación.')
        try:
            data = jwt.decode(token, app.config['SECRET_KEY'], algorithms=["HS256"])
            info.context.user = db.session.get(Usuario, data['id'])
            if not info.context.user: raise Exception('Usuario del token no encontrado.')
        except Exception as e:
            raise Exception(f'Token inválido: {str(e)}')
        return f(root, info, **kwargs)

    return decorated


def get_real_id(global_id, expected_type_name):
    type_name, real_id = from_global_id(global_id)
    if type_name != expected_type_name:
        raise Exception(f"ID inválido. Se esperaba '{expected_type_name}', se recibió '{type_name}'.")
    return int(real_id)


def add_column_if_missing(table, column, spec):
    inspector = inspect(db.engine)
    if column not in [c['name'] for c in inspector.get_columns(table)]:
        db.session.execute(text(f"ALTER TABLE {table} ADD COLUMN {column} {spec}"))
        db.session.commit()


def parse_schedule_string(schedule_str):
    days = {
        'lunes': 1, 'martes': 2, 'miercoles': 3, 'miércoles': 3,
        'jueves': 4, 'viernes': 5, 'sabado': 6, 'sábado': 6, 'domingo': 7
    }
    horarios = []
    if not schedule_str:
        return horarios
    for line in schedule_str.splitlines():
        line = line.strip()
        if not line:
            continue
        parts = line.split()
        day_name = parts[0].lower()
        if day_name not in days:
            continue
        times = line[len(parts[0]):].strip().split(' - ')
        if len(times) != 2:
            continue
        try:
            start = datetime.strptime(times[0], '%H:%M').time()
            end = datetime.strptime(times[1], '%H:%M').time()
        except Exception:
            continue
        horarios.append(Horario(dia_semana=days[day_name], hora_inicio=start, hora_fin=end))
    return horarios


def apply_migrations():
    add_column_if_missing('usuarios', 'updated_at',
                          'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP')
    add_column_if_missing('materias', 'created_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP')
    add_column_if_missing('materias', 'updated_at',
                          'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP')
    add_column_if_missing('tareas', 'created_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP')
    add_column_if_missing('tareas', 'updated_at',
                          'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP')
    add_column_if_missing('notas', 'created_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP')
    add_column_if_missing('notas', 'updated_at',
                          'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP')
    add_column_if_missing('horarios', 'created_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP')
    add_column_if_missing('horarios', 'updated_at',
                          'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP')


# --- 4. ESQUEMA GRAPHQL ---
class TareaType(SQLAlchemyObjectType):
    class Meta: model = Tarea; interfaces = (graphene.relay.Node,)

    db_id = graphene.Int(source='id')
    updated_at = graphene.DateTime()


class NotaType(SQLAlchemyObjectType):
    class Meta: model = Nota; interfaces = (graphene.relay.Node,)

    db_id = graphene.Int(source='id')
    updated_at = graphene.DateTime()


class HorarioType(SQLAlchemyObjectType):
    class Meta: model = Horario; interfaces = (graphene.relay.Node,)

    db_id = graphene.Int(source='id')
    updated_at = graphene.DateTime()


class MateriaType(SQLAlchemyObjectType):
    class Meta: model = Materia; interfaces = (graphene.relay.Node,)

    db_id = graphene.Int(source='id')
    updated_at = graphene.DateTime()
    horario = graphene.String()

    tareas = graphene.List(lambda: TareaType)
    notas = graphene.List(lambda: NotaType)
    horarios = graphene.List(lambda: HorarioType)

    tareas_count = graphene.Int()
    notas_count = graphene.Int()

    def resolve_tareas(self, info): return self.tareas

    def resolve_notas(self, info): return self.notas

    def resolve_horarios(self, info):
        return self.horarios

    def resolve_horario(self, info):
        days = {1: 'Lunes', 2: 'Martes', 3: 'Miercoles', 4: 'Jueves',
                5: 'Viernes', 6: 'Sabado', 7: 'Domingo'}
        lines = []
        for h in sorted(self.horarios, key=lambda h: (h.dia_semana, h.hora_inicio)):
            day = days.get(h.dia_semana, str(h.dia_semana))
            lines.append(f"{day} {h.hora_inicio.strftime('%H:%M')} - {h.hora_fin.strftime('%H:%M')}")
        return "\n".join(lines)

    def resolve_tareas_count(self, info):
        return db.session.query(func.count(Tarea.id)).filter_by(id_materia=self.id, id_usuario=self.id_usuario,
                                                                completada=False).scalar()

    def resolve_notas_count(self, info):
        return db.session.query(func.count(Nota.id)).filter_by(id_materia=self.id, id_usuario=self.id_usuario).scalar()


class DatosActualizados(graphene.ObjectType):
    materias = graphene.List(MateriaType)
    tareas = graphene.List(TareaType)
    notas = graphene.List(NotaType)
    horarios = graphene.List(HorarioType)


class Query(graphene.ObjectType):
    node = graphene.relay.Node.Field()
    mis_materias = graphene.List(MateriaType)
    todas_mis_tareas = graphene.List(TareaType)
    todas_mis_notas = graphene.List(NotaType)
    datos_actualizados = graphene.Field(DatosActualizados, desde=graphene.DateTime())

    @token_required
    def resolve_mis_materias(root, info):
        return db.session.query(Materia).filter_by(id_usuario=info.context.user.id).order_by(Materia.nombre).all()

    @token_required
    def resolve_todas_mis_tareas(root, info):
        return db.session.query(Tarea).filter_by(id_usuario=info.context.user.id).order_by(Tarea.titulo).all()

    @token_required
    def resolve_todas_mis_notas(root, info):
        return db.session.query(Nota).filter_by(id_usuario=info.context.user.id).order_by(Nota.titulo).all()

    @token_required
    def resolve_datos_actualizados(root, info, desde=None):
        uid = info.context.user.id

        q_materias = db.session.query(Materia).filter(Materia.id_usuario == uid)
        q_tareas = db.session.query(Tarea).filter(Tarea.id_usuario == uid)
        q_notas = db.session.query(Nota).filter(Nota.id_usuario == uid)
        q_horarios = db.session.query(Horario).join(Materia).filter(Materia.id_usuario == uid)

        if desde:
            q_materias = q_materias.filter(Materia.updated_at >= desde)
            q_tareas = q_tareas.filter(Tarea.updated_at >= desde)
            q_notas = q_notas.filter(Nota.updated_at >= desde)
            q_horarios = q_horarios.filter(Horario.updated_at >= desde)

        return DatosActualizados(
            materias=q_materias.all(),
            tareas=q_tareas.all(),
            notas=q_notas.all(),
            horarios=q_horarios.all(),
        )


# --- MUTACIONES ---
class CrearMateria(graphene.Mutation):
    class Arguments:
        nombre = graphene.String(required=True)
        profesor = graphene.String()
        horario = graphene.String()

    materia = graphene.Field(lambda: MateriaType)

    @token_required
    def mutate(root, info, nombre, **kwargs):
        schedule = kwargs.pop('horario', None)
        materia = Materia(nombre=nombre, id_usuario=info.context.user.id, **kwargs)
        db.session.add(materia)
        db.session.commit()
        for h in parse_schedule_string(schedule):
            h.id_materia = materia.id
            db.session.add(h)
        db.session.commit()
        return CrearMateria(materia=materia)


class ActualizarMateria(graphene.Mutation):
    class Arguments:
        id = graphene.ID(required=True)
        nombre = graphene.String()
        profesor = graphene.String()
        horario = graphene.String()

    materia = graphene.Field(lambda: MateriaType)

    @token_required
    def mutate(root, info, id, **kwargs):
        try:
            type_name, real_id = from_global_id(id)
            if type_name != 'MateriaType': raise Exception("ID de tipo incorrecto")
        except:
            real_id = int(id)

        materia = db.session.get(Materia, real_id)
        if not materia or materia.id_usuario != info.context.user.id:
            raise Exception("Materia no encontrada.")

        if 'nombre' in kwargs:
            materia.nombre = kwargs['nombre']
        if 'profesor' in kwargs:
            materia.profesor = kwargs['profesor']
        if 'horario' in kwargs:
            schedule = kwargs.pop('horario')
            for h in list(materia.horarios):
                db.session.delete(h)
            for h in parse_schedule_string(schedule):
                h.id_materia = materia.id
                db.session.add(h)

        db.session.commit()
        return ActualizarMateria(materia=materia)


class EliminarMateria(graphene.Mutation):
    class Arguments:
        id = graphene.ID(required=True)

    ok = graphene.Boolean()

    @token_required
    def mutate(root, info, id):
        try:
            type_name, real_id = from_global_id(id)
            if type_name != 'MateriaType': raise Exception("ID de tipo incorrecto")
        except:
            real_id = int(id)

        materia = db.session.get(Materia, real_id)
        if not materia or materia.id_usuario != info.context.user.id:
            raise Exception("Materia no encontrada.")

        # Eliminación en cascada de tareas, notas y horarios asociados
        for tarea in list(materia.tareas):
            db.session.delete(tarea)
        for nota in list(materia.notas):
            db.session.delete(nota)
        for horario in list(materia.horarios):
            db.session.delete(horario)

        db.session.delete(materia)
        db.session.commit()
        return EliminarMateria(ok=True)


class DesvincularYEliminarMateria(graphene.Mutation):
    class Arguments:
        id = graphene.ID(required=True)

    ok = graphene.Boolean()
    message = graphene.String()

    @token_required
    def mutate(root, info, id):
        try:
            type_name, real_id = from_global_id(id)
            if type_name != 'MateriaType': raise Exception("ID de tipo incorrecto")
        except:
            real_id = int(id)

        materia = db.session.get(Materia, real_id)
        if not materia or materia.id_usuario != info.context.user.id:
            raise Exception("Materia no encontrada.")

        nombre_materia = materia.nombre

        for tarea in list(materia.tareas):
            tarea.id_materia = None
        for nota in list(materia.notas):
            nota.id_materia = None

        db.session.delete(materia)
        db.session.commit()

        return DesvincularYEliminarMateria(ok=True,
                                           message=f"Materia '{nombre_materia}' eliminada y contenido desvinculado.")


class CrearTarea(graphene.Mutation):
    class Arguments:
        titulo = graphene.String(required=True)
        id_materia = graphene.ID(required=True)
        descripcion = graphene.String()
        fecha_entrega = graphene.DateTime()

    tarea = graphene.Field(lambda: TareaType)

    @token_required
    def mutate(root, info, titulo, id_materia, **kwargs):
        try:
            type_name, real_id_materia = from_global_id(id_materia)
            if type_name != 'MateriaType': raise Exception("ID de Materia inválido")
        except:
            real_id_materia = int(id_materia)

        if not db.session.get(Materia, real_id_materia):
            raise Exception("Materia no encontrada.")

        tarea = Tarea(titulo=titulo, id_materia=real_id_materia, id_usuario=info.context.user.id, **kwargs)
        db.session.add(tarea)
        db.session.commit()
        return CrearTarea(tarea=tarea)


class ActualizarTarea(graphene.Mutation):
    class Arguments:
        id = graphene.ID(required=True)
        titulo = graphene.String()
        descripcion = graphene.String()
        completada = graphene.Boolean()

    tarea = graphene.Field(lambda: TareaType)

    @token_required
    def mutate(root, info, id, **kwargs):
        try:
            type_name, real_id = from_global_id(id)
            if type_name != 'TareaType': raise Exception("ID de Tarea inválido")
        except:
            real_id = int(id)

        tarea = db.session.get(Tarea, real_id)
        if not tarea or tarea.id_usuario != info.context.user.id:
            raise Exception("Tarea no encontrada.")

        for key, value in kwargs.items():
            setattr(tarea, key, value)

        db.session.commit()
        return ActualizarTarea(tarea=tarea)


class EliminarTarea(graphene.Mutation):
    class Arguments:
        id = graphene.ID(required=True)

    ok = graphene.Boolean()

    @token_required
    def mutate(root, info, id):
        try:
            type_name, real_id = from_global_id(id)
            if type_name != 'TareaType': raise Exception("ID de Tarea inválido")
        except:
            real_id = int(id)

        tarea = db.session.get(Tarea, real_id)
        if not tarea or tarea.id_usuario != info.context.user.id:
            raise Exception("Tarea no encontrada.")

        db.session.delete(tarea)
        db.session.commit()
        return EliminarTarea(ok=True)


class CrearNota(graphene.Mutation):
    class Arguments:
        titulo = graphene.String(required=True)
        id_materia = graphene.ID(required=True)
        contenido = graphene.String()

    nota = graphene.Field(lambda: NotaType)

    @token_required
    def mutate(root, info, titulo, id_materia, **kwargs):
        try:
            type_name, real_id_materia = from_global_id(id_materia)
            if type_name != 'MateriaType': raise Exception("ID de Materia inválido")
        except:
            real_id_materia = int(id_materia)

        if not db.session.get(Materia, real_id_materia):
            raise Exception("Materia no encontrada.")

        nota = Nota(titulo=titulo, id_materia=real_id_materia, id_usuario=info.context.user.id, **kwargs)
        db.session.add(nota)
        db.session.commit()
        return CrearNota(nota=nota)


class ActualizarNota(graphene.Mutation):
    class Arguments:
        id = graphene.ID(required=True)
        titulo = graphene.String()
        contenido = graphene.String()

    nota = graphene.Field(lambda: NotaType)

    @token_required
    def mutate(root, info, id, **kwargs):
        try:
            type_name, real_id = from_global_id(id)
            if type_name != 'NotaType': raise Exception("ID de Nota inválido")
        except:
            real_id = int(id)

        nota = db.session.get(Nota, real_id)
        if not nota or nota.id_usuario != info.context.user.id:
            raise Exception("Nota no encontrada.")

        if 'titulo' in kwargs: nota.titulo = kwargs['titulo']
        if 'contenido' in kwargs: nota.contenido = kwargs['contenido']

        db.session.commit()
        return ActualizarNota(nota=nota)


class EliminarNota(graphene.Mutation):
    class Arguments:
        id = graphene.ID(required=True)

    ok = graphene.Boolean()

    @token_required
    def mutate(root, info, id):
        try:
            type_name, real_id = from_global_id(id)
            if type_name != 'NotaType': raise Exception("ID de Nota inválido")
        except:
            real_id = int(id)

        nota = db.session.get(Nota, real_id)
        if not nota or nota.id_usuario != info.context.user.id:
            raise Exception("Nota no encontrada.")

        db.session.delete(nota)
        db.session.commit()
        return EliminarNota(ok=True)


class CrearHorario(graphene.Mutation):
    class Arguments: id_materia = graphene.ID(required=True); dia_semana = graphene.Int(
        required=True); hora_inicio = graphene.Time(required=True); hora_fin = graphene.Time(required=True)

    horario = graphene.Field(lambda: HorarioType)

    @token_required
    def mutate(root, info, id_materia, **kwargs):
        real_id_materia = get_real_id(id_materia, 'MateriaType')
        if not db.session.get(Materia, real_id_materia): raise Exception("Materia no encontrada.")
        horario = Horario(id_materia=real_id_materia, **kwargs)
        db.session.add(horario);
        db.session.commit()
        return CrearHorario(horario=horario)


class ActualizarHorario(graphene.Mutation):
    class Arguments:
        id = graphene.ID(required=True)
        dia_semana = graphene.Int()
        hora_inicio = graphene.Time()
        hora_fin = graphene.Time()

    horario = graphene.Field(lambda: HorarioType)

    @token_required
    def mutate(root, info, id, **kwargs):
        real_id = get_real_id(id, 'HorarioType')
        horario = db.session.get(Horario, real_id)
        if not horario or horario.materia.usuario.id != info.context.user.id:
            raise Exception("Horario no encontrado.")
        for key, value in kwargs.items():
            setattr(horario, key, value)
        db.session.commit()
        return ActualizarHorario(horario=horario)


class EliminarHorario(graphene.Mutation):
    class Arguments:
        id = graphene.ID(required=True)

    ok = graphene.Boolean()

    @token_required
    def mutate(root, info, id):
        real_id = get_real_id(id, 'HorarioType')
        horario = db.session.get(Horario, real_id)
        if not horario or horario.materia.usuario.id != info.context.user.id:
            raise Exception("Horario no encontrado.")
        db.session.delete(horario)
        db.session.commit()
        return EliminarHorario(ok=True)


class Mutations(graphene.ObjectType):
    crear_materia = CrearMateria.Field()
    actualizar_materia = ActualizarMateria.Field()
    eliminar_materia = EliminarMateria.Field()
    desvincular_y_eliminar_materia = DesvincularYEliminarMateria.Field()
    crear_tarea = CrearTarea.Field()
    actualizar_tarea = ActualizarTarea.Field()
    eliminar_tarea = EliminarTarea.Field()
    crear_nota = CrearNota.Field()
    actualizar_nota = ActualizarNota.Field()
    eliminar_nota = EliminarNota.Field()
    crear_horario = CrearHorario.Field()
    actualizar_horario = ActualizarHorario.Field()
    eliminar_horario = EliminarHorario.Field()


schema = graphene.Schema(query=Query, mutation=Mutations)


# --- 5. RUTAS DE LA APLICACIÓN (Flask) ---
@app.route('/api/register', methods=['POST'])
def register_user():
    data = request.get_json()
    if not data or not data.get('nombre_usuario') or not data.get('contrasena'): return jsonify(
        {'message': 'Datos incompletos'}), 400
    if db.session.query(Usuario).filter_by(nombre_usuario=data.get('nombre_usuario')).first(): return jsonify(
        {'message': 'El nombre de usuario ya existe'}), 409
    nuevo_usuario = Usuario(data.get('nombre_usuario'), data.get('contrasena'))
    db.session.add(nuevo_usuario);
    db.session.commit()
    return jsonify({'message': 'Usuario registrado exitosamente'}), 201


@app.route('/api/login', methods=['POST'])
def login_user():
    data = request.get_json()
    if not data or not data.get('nombre_usuario') or not data.get('contrasena'): return jsonify(
        {'message': 'Datos incompletos'}), 400
    usuario = db.session.query(Usuario).filter_by(nombre_usuario=data.get('nombre_usuario')).first()
    if not usuario or not usuario.verificar_contrasena(data.get('contrasena')): return jsonify(
        {'message': 'Credenciales inválidas'}), 401
token = jwt.encode(
    {
        'id': usuario.id,
        'exp': datetime.now().astimezone() + timedelta(hours=24)
    },
    app.config['SECRET_KEY'],
    algorithm="HS256"
)
return jsonify({'token': token})


@app.route('/api/subir-imagen', methods=['POST'])
def subir_imagen():
    if 'imagen' not in request.files:
        return jsonify({'error': 'No se encontró la imagen'}), 400
    imagen = request.files['imagen']
    nombre = secure_filename(imagen.filename)
    os.makedirs('imagenes', exist_ok=True)
    ruta = os.path.join('imagenes', nombre)
    imagen.save(ruta)
    nueva = Imagen(ruta=ruta)
    db.session.add(nueva)
    db.session.commit()
    return jsonify({'mensaje': 'Imagen guardada', 'ruta': ruta})


@app.route('/imagenes/<path:filename>')
def imagenes(filename):
    return send_from_directory('imagenes', filename)


@app.route("/graphql", methods=["POST"])
def graphql_server():
    data = request.get_json()
    result = schema.execute(data.get("query"), context_value=request, variable_values=data.get('variables'))
    response = {}
    if result.errors:
        error_messages = []
        for err in result.errors:
            original = getattr(err, 'original_error', None)
            if original:
                # Loguea el error completo en la consola del servidor para depuración
                app.logger.error(f"GraphQL Error: {repr(original)}")
                error_messages.append(f"Error Interno: {str(original)}")
            else:
                error_messages.append(str(err))
        response['errors'] = error_messages
    if result.data:
        response['data'] = result.data
    # Devuelve 200 si hay datos, incluso si también hay errores parciales.
    # Devuelve 400 solo si la petición falla por completo y no hay datos.
    status_code = 200 if result.data else 400
    return jsonify(response), status_code


# --- PUNTO DE ENTRADA ---
if __name__ == '__main__':
    with app.app_context():
        db.create_all()
        apply_migrations()
    app.run(host='0.0.0.0', port=5000, debug=True)
