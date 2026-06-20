# FMPRO — Football Manager Pro

Gestión de equipos de fútbol con **backend Spring Boot** y **frontend React**, todo en tu PC: sin Heroku ni base de datos en la nube.

## Requisitos

- Java 17+ (probado con Java 25)
- Node.js 18+

## Arranque rápido

### 1. Backend (API + base de datos local)

```powershell
.\scripts\start-backend.ps1
```

Si ves **"Port 8080 was already in use"**, otra instancia sigue abierta:

```powershell
.\scripts\stop-backend.ps1
.\gradlew.bat bootRun
```

- API: http://localhost:8080  
- Los datos se guardan en la carpeta `data/` (H2 en disco).  
- Consola H2: http://localhost:8080/h2-console  
  - JDBC: `jdbc:h2:file:./data/fmpro`  
  - Usuario: `sa`  
  - Contraseña: *(vacía)*

### 2. Frontend

```powershell
cd frontend
npm install
npm run dev
```

Abre la URL que muestre Vite (normalmente http://localhost:5173). El frontend ya apunta a `http://localhost:8080`.

## Estructura

| Componente | Tecnología | Ubicación datos |
|------------|------------|-----------------|
| API REST | Spring Boot 3.4 | — |
| Base de datos | H2 (archivo) | `./data/fmpro.*` |
| Interfaz web | React + Vite | — |

## API principal

- `POST /api/auth/register`, `POST /api/auth/login`
- `GET/POST/PUT/DELETE /api/teams`
- `GET/POST/PUT/DELETE /api/players`
- `GET/POST/DELETE /api/matches`
- `GET/POST/PUT/DELETE /api/statistics`
- `GET /api/reports/players/{id}` (PDF)

## Acceso

El panel exige iniciar sesión. Al registrarse solo puedes elegir **Usuario** o **Entrenador** (el rol administrador se crea al arrancar el backend en local).

Tras reiniciar el backend, el usuario `admin` queda con contraseña `admin` (solo entorno local).

## Scripts útiles

```powershell
# Solo backend
.\scripts\start-backend.ps1

# Solo frontend (con backend ya en marcha)
.\scripts\start-frontend.ps1
```

## Notas

- La carpeta `data/` está en `.gitignore`: tus datos no se suben a Git.  
- El backend **no** sirve HTML: usa siempre `frontend/` con `npm run dev`.
