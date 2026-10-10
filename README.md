# 🧠 Focus Filter — Herramienta de Autocontrol y Enfoque Personal

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?style=flat&logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Database-Room-orange.svg?style=flat&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

**Focus Filter** es una aplicación Android nativa diseñada para fomentar el autocontrol personal y reducir las decisiones impulsivas frente a las distracciones digitales. 

A diferencia de las aplicaciones tradicionales de control parental, **Focus Filter** se enfoca en la **atención plena (mindfulness)** y la **reflexión intencional**: el usuario decide voluntariamente qué sitios bloquear e introduce un tiempo de espera deliberado de **60 segundos** antes de poder desactivar la protección o eliminar un sitio bloqueado.

---

## 📸 Capturas de Pantalla

> **Instrucciones:** Coloca las imágenes correspondientes en la carpeta `.github/images/` dentro del repositorio con los nombres indicados abajo.

| Pantalla Principal (Inactiva) | Pantalla Principal (Activa) | Retardo de 60s (Reflexión) | Administración de Sitios |
| :---: | :---: | :---: | :---: |
| ![Main Inactive](.github/images/main_screen_inactive.png) | ![Main Active](.github/images/main_screen_active.png) | ![Hold Delay](.github/images/hold_delay.png) | ![Blocked Sites](.github/images/blocked_sites.png) |

---

## ✨ Características Principales

- 🛑 **Filtro de Sitios Web 100% Local**:
  - Utiliza `VpnService` de Android para interceptar y resolver consultas DNS de manera local.
  - **Sin servidores VPN remotos**: El tráfico regular (YouTube, Google, descargas, HTTPS/TCP) fluye directamente a través de tu red Wi-Fi o datos móviles a máxima velocidad sin perder conectividad.

- ⏳ **Protección de 60 Segundos contra Decisiones Impulsivas**:
  - Para desactivar la VPN o eliminar un dominio de la lista negra, debes **mantener presionado el botón continuamente durante 60 segundos**.
  - Si sueltas el botón antes de tiempo, el proceso se cancela inmediatamente y vuelve a cero.
  - Muestra un temporizador regresivo, porcentaje de progreso y frases motivacionales/reflexivas para ayudarte a pausar antes de actuar.

- 💾 **Gestión de Dominios con Persistencia Local (Room)**:
  - Agrega y administra libremente tus sitios bloqueados (`reddit.com`, `instagram.com`, `x.com`, `facebook.com`, etc.).
  - Persistencia local en SQLite/Room que almacena el dominio y la fecha de creación.

- 🔄 **Reinicio Automático y Always-On VPN**:
  - Escucha el evento `BOOT_COMPLETED` para restaurar automáticamente el estado de la VPN tras reiniciar o encender el dispositivo.
  - Acceso rápido e integración con la función nativa de Android **VPN siempre activa** (*Always-On VPN*).

- 🎨 **Diseño Sereno y Tono de Autocontrol**:
  - Construido con **Jetpack Compose** y **Material 3**.
  - Paleta de colores calma (tonos azul pizarra y serenos) orientada a la concentración, sin lenguaje punitivo o de vigilancia.
  - **Sin PINs, contraseñas ni bloqueos parentales**.

---

## 🛠️ Arquitectura y Tecnologías

Focus Filter está desarrollado siguiendo los estándares modernos de desarrollo Android:

- **Lenguaje**: [Kotlin 2.0+](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) con **Material 3**
- **Arquitectura**: MVVM (Model-View-ViewModel) + StateFlow & Coroutines
- **Base de Datos Local**: [Room Database](https://developer.android.com/training/data-storage/room) con [KSP](https://developer.android.com/build/migrate-to-ksp)
- **Red y Filtrado**: Android `VpnService` con captura selectiva de paquetes DNS (UDP puerto 53)
- **Asíncronos & Hilos**: Kotlin Coroutines & Flow

---

## 📂 Estructura del Proyecto

```text
app/src/main/java/com/example/nmf/
├── data/
│   ├── BlockedSite.kt          # Entidad de sitio bloqueado
│   ├── BlockedSiteDao.kt       # DAO de Room para consultas
│   └── AppDatabase.kt          # Base de datos local
├── ui/
│   ├── components/
│   │   └── HoldToConfirmButton.kt # Botón con pulsación continua de 60s
│   ├── screens/
│   │   └── BlockedSitesScreen.kt  # Pantalla de administración de dominios
│   └── FocusFilterViewModel.kt    # ViewModel de estado de la UI
├── BootReceiver.kt             # Receptor para reinicio automático (BOOT_COMPLETED)
├── MainActivity.kt             # Actividad principal e interfaz Compose
├── MyVpnService.kt             # Servicio VPN local para intercepción DNS
└── VpnPreferences.kt           # Gestor de preferencias persistentes (SharedPreferences)
```

---

## 🚀 Instalación y Compilación

1. **Clonar el repositorio**:
   ```bash
   git clone https://github.com/tu-usuario/focus-filter.git
   cd focus-filter
   ```

2. **Abrir en Android Studio**:
   - Abre Android Studio (Ladybug 2024.2.1 o superior recomendado).
   - Deja que Gradle sincronice el proyecto y descargue las dependencias.

3. **Requisitos del Entorno**:
   - **Compile SDK**: 35 (Android 15)
   - **Min SDK**: 24 (Android 7.0)
   - **JDK**: Java 17

4. **Compilar e Instalar**:
   - Conecta un dispositivo Android o inicia un emulador.
   - Presiona `Run` en Android Studio o ejecuta desde la terminal:
     ```bash
     ./gradlew assembleDebug
     ```

---

## 🔒 Privacidad y Seguridad

Focus Filter es una herramienta enfocada 100% en la privacidad del usuario:

- 🛡️ **Todo permanece en tu dispositivo**: Ningún dato de navegación ni dominio bloqueado se envía a servidores externos.
- ⚡ **Sin VPN remota**: La VPN se ejecuta exclusivamente de manera local en el procesador del teléfono.
- 🌐 **Tráfico seguro**: Todo el tráfico Web/HTTPS normal fluye directamente a través de tu proveedor de red sin ser registrado ni modificado.

---

## 📝 Licencia

Este proyecto se distribuye bajo la licencia **MIT**. Consulta el archivo `LICENSE` para obtener más información.
