# EMT Real-Time

Real-time public transport application for **EMT Madrid** that provides estimated bus arrival times for a specific bus stop and optionally filters results by bus line.

The project combines a **Python backend**, an **Android application built with Kotlin and Jetpack Compose**, the **EMT Madrid Mobility Labs API**, local persistence for user favourites, automatic data refresh and Firebase Cloud Messaging integration.

The project was developed as a practical end-to-end engineering project covering API integration, backend development, Android development, asynchronous processing, local persistence, testing and software architecture.

---

## Table of Contents

* [Overview](#overview)
* [Features](#features)
* [Architecture](#architecture)
* [Project Structure](#project-structure)
* [Technology Stack](#technology-stack)
* [How It Works](#how-it-works)
* [Backend](#backend)
* [Android Application](#android-application)
* [EMT API Integration](#emt-api-integration)
* [Real-Time Data Flow](#real-time-data-flow)
* [Favorites](#favorites)
* [Automatic Refresh](#automatic-refresh)
* [Connection Error Handling](#connection-error-handling)
* [Firebase Cloud Messaging](#firebase-cloud-messaging)
* [Data Model](#data-model)
* [API Endpoints](#api-endpoints)
* [Configuration](#configuration)
* [Running the Backend](#running-the-backend)
* [Running the Android Application](#running-the-android-application)
* [Testing](#testing)
* [Development Workflow](#development-workflow)
* [Git Workflow](#git-workflow)
* [Security Considerations](#security-considerations)
* [Current Status](#current-status)
* [Known Limitations](#known-limitations)
* [Future Improvements](#future-improvements)
* [Engineering Decisions](#engineering-decisions)
* [Learning Objectives](#learning-objectives)
* [License](#license)

---

# Overview

**EMT Real-Time** is an end-to-end application designed to provide real-time information about bus arrivals in Madrid.

The application allows a user to:

* Enter an EMT Madrid bus stop.
* Optionally enter a bus line.
* Query current estimated arrivals.
* Display the results in a mobile-friendly interface.
* Automatically refresh arrival information.
* Save a bus stop and line as a favourite.
* Restore a favourite with a single tap.
* Continue displaying the last available data when the backend temporarily loses connectivity.
* Display the time of the latest successful update.
* Receive Firebase Cloud Messaging infrastructure through the Android application.

The project consists of two main components:

```text
┌──────────────────────────────┐
│        Android App           │
│                              │
│ Kotlin + Jetpack Compose     │
│ Favorites + UI + FCM         │
└──────────────┬───────────────┘
               │
               │ HTTP / REST
               ▼
┌──────────────────────────────┐
│       Python Backend         │
│                              │
│ FastAPI + EMT Client         │
└──────────────┬───────────────┘
               │
               │ API
               ▼
┌──────────────────────────────┐
│       EMT Madrid API         │
│      Mobility Labs API       │
└──────────────────────────────┘
```

---

# Features

## Bus Arrival Search

Users can enter a bus stop number and retrieve the estimated arrival information provided by EMT Madrid.

Example:

```text
Stop: 72
```

The application retrieves the available arrivals and displays information such as:

* Bus line
* Destination
* Distance
* Estimated arrival time

---

## Bus Line Filtering

The user can optionally specify a bus line.

For example:

```text
Stop: 72
Line: 27
```

The Android application retrieves the arrivals for the stop and filters the displayed results to the selected line.

The filtering is performed locally on the returned arrival collection.

---

## Automatic Refresh

The application automatically refreshes the information every **30 seconds**.

This allows the user to keep the application open while waiting for a bus without manually pressing the search button.

```text
Initial request
      │
      ▼
Display arrivals
      │
      ▼
Wait 30 seconds
      │
      ▼
Request updated data
      │
      ▼
Update UI
      │
      └───────────────► repeat
```

---

## Favorites

Users can save a specific:

* Bus stop
* Bus line
* Destination

as a favourite.

For example:

```text
⭐ My favourite

Stop 72 · Line 27
Plaza de Castilla
```

Selecting the favourite automatically restores:

```text
stopInput = 72
lineInput = 27
stopId = 72
```

and triggers a new request.

This means the favourite is not simply stored as information: it acts as a **quick-access configuration**.

---

## Local Persistence

Favorites are persisted locally using Android `SharedPreferences`.

The application can therefore restore the saved favourite when it is started again.

The storage layer is encapsulated in:

```text
FavoriteStorage
```

and the favourite model is represented by:

```text
Favorite
```

---

## Connection Error Handling

The application is designed to avoid unnecessarily losing useful information when a temporary connection failure occurs.

If an update fails after previous data has already been retrieved, the application keeps displaying the previously available arrivals and shows:

```text
⚠ Sin conexión · mostrando los últimos datos disponibles
```

This provides a better user experience than replacing the entire screen with an error.

When the next request succeeds, the error state is cleared automatically.

---

## Last Successful Update

The application displays the time of the latest successful request.

Example:

```text
Última actualización: 17:27:32
```

This is particularly useful when the application is displaying cached/previous results because it allows the user to understand how recent those results are.

---

## Firebase Cloud Messaging

The Android application includes Firebase Cloud Messaging integration.

The application obtains an FCM registration token and requests notification permission on supported Android versions.

This provides the infrastructure required for future push-notification functionality.

Potential future use cases include:

* Bus arrival notifications.
* Favourite stop notifications.
* Service alerts.
* Transport disruption notifications.

---

# Architecture

The project follows a client/backend architecture.

```text
                         ┌─────────────────────┐
                         │     EMT Madrid      │
                         │    Mobility Labs    │
                         └──────────┬──────────┘
                                    │
                              External API
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   Python Backend    │
                         │                     │
                         │ EMT Client          │
                         │ Arrival Service     │
                         │ FastAPI              │
                         │ Pydantic Models      │
                         └──────────┬──────────┘
                                    │
                              REST / HTTP
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │    Android App      │
                         │                     │
                         │ Kotlin              │
                         │ Jetpack Compose     │
                         │ Retrofit            │
                         │ SharedPreferences   │
                         │ Firebase Messaging  │
                         └─────────────────────┘
```

The architecture deliberately separates:

1. External data acquisition.
2. Backend API.
3. Mobile presentation.
4. Local application state.
5. Local persistence.
6. Notification infrastructure.

---

# Project Structure

The repository is organized as follows:

```text
emt-real-time/
│
├── android/
│   │
│   ├── app/
│   │   ├── src/
│   │   │   ├── androidTest/
│   │   │   ├── main/
│   │   │   │   ├── java/
│   │   │   │   │   └── com/
│   │   │   │   │       └── tomasperez/
│   │   │   │   │           └── emtrealtime/
│   │   │   │   │               ├── MainActivity.kt
│   │   │   │   │               ├── MyFirebaseMessagingService.kt
│   │   │   │   │               ├── RetrofitClient.kt
│   │   │   │   │               │
│   │   │   │   │               ├── data/
│   │   │   │   │               │   ├── BusArrival.kt
│   │   │   │   │               │   ├── EmtApi.kt
│   │   │   │   │               │   └── Favorite.kt
│   │   │   │   │               │
│   │   │   │   │               ├── storage/
│   │   │   │   │               │   └── FavoriteStorage.kt
│   │   │   │   │               │
│   │   │   │   │               └── ui/
│   │   │   │   │                   └── theme/
│   │   │   │   │                       ├── Color.kt
│   │   │   │   │                       ├── Theme.kt
│   │   │   │   │                       └── Type.kt
│   │   │   │   │
│   │   │   │   └── AndroidManifest.xml
│   │   │   │
│   │   │   └── test/
│   │   │
│   │   └── build.gradle.kts
│   │
│   ├── build.gradle.kts
│   ├── gradle.properties
│   ├── gradle/
│   ├── gradlew
│   ├── gradlew.bat
│   └── settings.gradle.kts
│
├── src/
│   └── emt_realtime/
│       ├── __init__.py
│       ├── arrival_service.py
│       ├── emt_client.py
│       ├── models.py
│       │
│       ├── api/
│       │   ├── __init__.py
│       │   └── main.py
│       │
│       └── test_arrivals.py
│
├── tests/
│
├── pyproject.toml
├── .gitignore
└── README.md
```

---

# Technology Stack

## Backend

| Technology | Purpose                     |
| ---------- | --------------------------- |
| Python     | Backend implementation      |
| FastAPI    | REST API                    |
| Pydantic   | Data modelling / validation |
| Requests   | External HTTP communication |
| Uvicorn    | ASGI server                 |
| Pytest     | Testing                     |

---

## Android

| Technology               | Purpose                          |
| ------------------------ | -------------------------------- |
| Kotlin                   | Application language             |
| Jetpack Compose          | UI                               |
| Material 3               | UI components                    |
| Retrofit                 | HTTP client                      |
| Firebase Cloud Messaging | Push notification infrastructure |
| SharedPreferences        | Local favourite persistence      |
| Gradle                   | Android build system             |

---

## Development

| Tool           | Purpose                       |
| -------------- | ----------------------------- |
| Git            | Version control               |
| GitHub         | Source code hosting           |
| Android Studio | Android development           |
| VS Code        | Backend development           |
| Conda          | Python environment management |

---

# How It Works

The complete request flow is:

```text
User
 │
 │ enters stop
 ▼
Android UI
 │
 │ HTTP request
 ▼
Python Backend
 │
 │ calls EMT API
 ▼
EMT Madrid
 │
 │ arrival data
 ▼
Python Backend
 │
 │ structured response
 ▼
Android
 │
 │ update state
 ▼
Jetpack Compose
 │
 ▼
Arrival Cards
```

For example:

```text
User enters:

Stop = 72
Line = 27
```

The Android application requests the arrivals for stop `72`.

The backend obtains the current data from EMT Madrid.

The Android application receives the collection and applies the optional line filter:

```kotlin
arrivals.filter {
    it.line.equals(
        lineInput.trim(),
        ignoreCase = true
    )
}
```

The resulting data is displayed through reusable arrival cards.

---

# Backend

The backend is implemented in Python.

Its responsibilities include:

* Communicating with the EMT API.
* Handling authentication.
* Retrieving arrival data.
* Converting external responses into application models.
* Exposing a REST API for the Android client.

Main components:

```text
emt_client.py
    │
    └── EMT API communication

arrival_service.py
    │
    └── Arrival-related application logic

models.py
    │
    └── Data models

api/main.py
    │
    └── FastAPI application
```

---

# Android Application

The Android application uses **Kotlin** and **Jetpack Compose**.

The main activity maintains the application state, including:

```text
stopId
stopInput
lineInput
arrivals
errorMessage
lastUpdate
loading
fcmToken
favorite
```

Compose observes changes to this state and updates the UI automatically.

For example:

```kotlin
private var arrivals by mutableStateOf<List<BusArrival>>(emptyList())
```

When `arrivals` changes, Compose recomposes the relevant UI.

This provides a reactive UI model rather than manually manipulating views.

---

# EMT API Integration

The project integrates with the EMT Madrid **Mobility Labs** API.

The application uses the API to obtain estimated bus arrivals for a specific stop.

The integration requires application credentials issued through the EMT Mobility Labs platform.

Credentials should **never be hard-coded into application source code**.

The general architecture is:

```text
Android
   │
   ▼
Python Backend
   │
   ├── authentication
   │
   └── arrival request
          │
          ▼
      EMT API
```

Keeping the external API interaction in the backend provides a clear separation between the mobile client and the external service.

---

# Real-Time Data Flow

The application treats arrival information as dynamic data.

A typical update cycle is:

```text
GET arrivals
     │
     ▼
Receive data
     │
     ▼
Update arrivals state
     │
     ▼
Update lastUpdate
     │
     ▼
Compose recomposition
     │
     ▼
Refresh cards
```

Every 30 seconds the cycle is repeated.

If the request fails:

```text
GET arrivals
     │
     X
 Connection failure
     │
     ▼
Keep previous arrivals
     │
     ▼
Display connection warning
```

When connectivity returns:

```text
GET arrivals
     │
     ▼
Successful response
     │
     ▼
Replace arrivals
     │
     ▼
Clear error
```

---

# Favorites

The favourite model is:

```kotlin
data class Favorite(
    val stopId: Int,
    val line: String,
    val destination: String
)
```

A favourite therefore contains the minimum information needed to restore a user's preferred journey.

Example:

```text
stopId      = 72
line        = 27
destination = Plaza de Castilla
```

---

## FavoriteStorage

Favorites are persisted using Android `SharedPreferences`.

The storage abstraction provides:

```text
saveFavorite()
getFavorite()
deleteFavorite()
```

This keeps persistence logic separate from the UI.

The lifecycle is:

```text
User searches
     │
     ▼
Arrival found
     │
     ▼
Save as favourite
     │
     ▼
FavoriteStorage
     │
     ▼
SharedPreferences
```

On application startup:

```text
Application starts
     │
     ▼
FavoriteStorage.getFavorite()
     │
     ▼
Restore favourite
     │
     ▼
Display favourite card
```

---

# Favorite Quick Access

A favourite can be selected directly from the favourite card.

When selected:

```text
Favorite
   │
   ├── stopId ─────► stopInput
   │
   ├── line ───────► lineInput
   │
   └── stopId ─────► stopId
                         │
                         ▼
                    loadArrivals()
```

This avoids repeatedly entering the same stop and line manually.

---

# Automatic Refresh

The application starts an automatic refresh coroutine:

```kotlin
while (true) {

    delay(30_000)

    loadArrivals(stopId)
}
```

The current stop is therefore refreshed every 30 seconds.

This approach was selected because the project is designed around near-real-time arrival information.

---

# Loading State

The UI maintains a loading state:

```kotlin
private var loading by mutableStateOf(false)
```

Before a request:

```text
loading = true
```

After completion:

```text
loading = false
```

This allows the interface to communicate that a request is currently being processed.

---

# Connection Error Handling

A key UX decision is that a failed refresh does **not automatically delete previously retrieved data**.

Instead:

```text
Previous arrivals
       │
       │ refresh
       ▼
   Request fails
       │
       ├── keep arrivals
       │
       └── display warning
```

The application therefore distinguishes between:

* **No data available yet**
* **Existing data but current refresh failed**

This is important for real-time applications because temporary network problems should not unnecessarily remove information that is still useful.

---

# Last Update Timestamp

The timestamp is updated only after a successful request:

```kotlin
lastUpdate = SimpleDateFormat(
    "HH:mm:ss",
    Locale.getDefault()
).format(Date())
```

This means the displayed timestamp represents the latest successful retrieval rather than the latest attempted request.

---

# Firebase Cloud Messaging

The Android application integrates Firebase Cloud Messaging.

The application obtains an FCM token through:

```kotlin
FirebaseMessaging.getInstance()
    .token
```

The application also requests the notification permission on Android versions where it is required.

The current implementation establishes the infrastructure for future notification functionality.

---

# Data Model

The Android application uses a `BusArrival` model to represent the arrival information returned by the backend.

Arrival cards currently expose information such as:

```text
Line
Destination
Distance
Minutes
```

Example:

```text
┌─────────────────────────────┐
│ Línea 27              5 min │
│ Plaza de Castilla           │
│ 850 m                       │
└─────────────────────────────┘
```

---

# Arrival Cards

Arrival information is rendered through a reusable Compose component:

```kotlin
ArrivalCard(
    arrival = arrival
)
```

The component is responsible only for presentation.

This makes the UI reusable and easier to evolve independently from the API communication layer.

---

# API Endpoints

The backend exposes the application API through FastAPI.

The Android application communicates with the backend rather than directly coupling the mobile UI to the external EMT API.

The exact endpoint configuration is defined in the backend and Android Retrofit client.

Typical application flow:

```text
GET /arrivals/{stop}
```

Conceptually:

```text
GET /arrivals/72
```

returns the available arrival information for stop `72`.

The API layer can be inspected through FastAPI's automatically generated documentation when running the development server.

---

# Configuration

## Python Environment

The backend is designed to run in a Python environment.

A Conda environment can be created with:

```bash
conda create -n emt-realtime python=3.12 -y
```

Activate it:

```bash
conda activate emt-realtime
```

---

## Python Dependencies

Install the project in editable mode:

```bash
pip install -e .
```

Development dependencies can be installed according to the configuration in:

```text
pyproject.toml
```

---

# Running the Backend

From the repository root:

```bash
cd ~/Proyectos/emt-real-time
```

Activate the environment:

```bash
conda activate emt-realtime
```

Start the FastAPI application with Uvicorn.

For example:

```bash
uvicorn emt_realtime.api.main:app --reload
```

The backend must be running before the Android application can retrieve arrival information.

Development architecture:

```text
Android
   │
   │ HTTP
   ▼
localhost backend
   │
   ▼
EMT Madrid API
```

---

# Running the Android Application

Open the `android` directory in Android Studio:

```text
emt-real-time/android
```

Android Studio can then synchronize the Gradle project.

The application can be built using:

```text
Build
→ Assemble Project
```

or from the command line:

```bash
./gradlew assembleDebug
```

On Windows:

```bash
gradlew.bat assembleDebug
```

After a successful build, the application can be run on:

* Android emulator
* Physical Android device

---

# Backend + Android Development Workflow

The complete development workflow is:

### Terminal 1 — Backend

```bash
cd ~/Proyectos/emt-real-time

conda activate emt-realtime

uvicorn emt_realtime.api.main:app --reload
```

### Android Studio

Open:

```text
~/Proyectos/emt-real-time/android
```

Then:

```text
Build
→ Assemble Project
```

and:

```text
Run
```

The Android application then communicates with the running backend.

---

# Testing

The project includes Python tests.

The test suite can be executed with:

```bash
pytest
```

The backend test code includes arrival-related testing.

The Android project also contains the standard Android unit and instrumentation test structure.

Future iterations can expand the test coverage around:

* EMT API client
* API response parsing
* Arrival service
* FastAPI endpoints
* Favorite storage
* Favourite restoration
* Connection failures
* Empty API responses
* Invalid stop numbers
* Android UI states

---

# Development Workflow

The project is developed incrementally.

Typical workflow:

```text
Implement
   │
   ▼
Run backend tests
   │
   ▼
Run backend
   │
   ▼
Build Android application
   │
   ▼
Run on device
   │
   ▼
Test real API behaviour
   │
   ▼
git status
   │
   ▼
git diff
   │
   ▼
git commit
   │
   ▼
git push
```

This keeps the repository synchronized with the working application.

---

# Git Workflow

The repository uses Git with `main` as the primary branch.

Remote repository:

```text
github.com/Megapixel777/emt-real-time
```

Typical workflow:

```bash
git status
```

Review changes:

```bash
git diff
```

Stage changes:

```bash
git add .
```

Create a commit:

```bash
git commit -m "Description of change"
```

Push:

```bash
git push
```

Verify:

```bash
git status
```

Expected clean state:

```text
nothing to commit, working tree clean
```

---

# Security Considerations

The application communicates with external services that require credentials.

Credentials should not be hard-coded into Python source files or committed as plain text.

Sensitive configuration should be provided through environment variables or local configuration.

Examples:

```bash
export EMT_CLIENT_ID="..."
export EMT_API_KEY="..."
```

Do not publish real credentials in:

* README files
* Python source code
* Kotlin source code
* Git history
* screenshots
* documentation
* public repositories

If a credential has accidentally been committed, it should be considered exposed and rotated through the relevant provider.

---

## Firebase Configuration

The Android project uses Firebase configuration for the application.

The Firebase Android configuration file is:

```text
android/app/google-services.json
```

Before making the repository public, Firebase configuration and API-key restrictions should be reviewed.

Where appropriate, Firebase keys should be restricted to the intended Android application and package.

---

# Current Status

The current version of the project provides a functional end-to-end proof of concept.

Implemented functionality includes:

* [x] EMT Madrid API integration
* [x] Python backend
* [x] FastAPI REST layer
* [x] Android application
* [x] Kotlin
* [x] Jetpack Compose UI
* [x] Retrofit HTTP client
* [x] Bus stop search
* [x] Optional line filtering
* [x] Arrival cards
* [x] Automatic refresh
* [x] Last successful update timestamp
* [x] Loading state
* [x] Connection error handling
* [x] Previous-data fallback after connection failures
* [x] Favourite persistence
* [x] Favourite quick access
* [x] Favourite deletion
* [x] Firebase Cloud Messaging integration
* [x] Notification permission handling
* [x] Python tests
* [x] Git repository
* [x] GitHub repository

---

# Known Limitations

The project is currently a proof of concept and has several areas that can be improved.

## Single Favourite

The current implementation stores one favourite.

A future version could support:

```text
⭐ Home
⭐ Work
⭐ Gym
⭐ University
```

with multiple saved stops.

---

## Backend Availability

The Android application depends on the Python backend being available.

If the backend is not running, the Android application cannot obtain new data.

The application currently handles this gracefully by keeping previously retrieved arrival information where available.

---

## Real-Time Guarantees

Arrival information is dependent on the external EMT API.

The application therefore cannot guarantee that an arrival estimate represents an exact physical arrival time.

The application displays the information returned by the external service.

---

## Background Execution

The current automatic refresh mechanism is designed for an active application session.

A production implementation could use Android background scheduling mechanisms where appropriate.

---

# Future Improvements

The project provides a foundation for several additional engineering improvements.

## Multiple Favorites

Replace the single favourite with a collection:

```text
List<Favorite>
```

Potential UI:

```text
⭐ Home
⭐ Work
⭐ Gym
```

---

## ViewModel Architecture

Move application state and business logic out of `MainActivity`.

Target architecture:

```text
MainActivity
     │
     ▼
ViewModel
     │
     ├── Repository
     │
     └── API
```

This would improve:

* Separation of concerns
* Testability
* Lifecycle handling
* Maintainability

---

## Repository Pattern

Introduce:

```text
Repository
```

between the ViewModel and Retrofit.

For example:

```text
UI
 │
 ▼
ViewModel
 │
 ▼
Repository
 │
 ├── Retrofit
 │
 └── Local storage
```

---

## Improved State Management

Introduce an explicit UI state:

```kotlin
sealed class ArrivalUiState {

    data object Loading : ArrivalUiState()

    data class Success(
        val arrivals: List<BusArrival>
    ) : ArrivalUiState()

    data class Error(
        val message: String
    ) : ArrivalUiState()
}
```

This would make loading, success and error states more explicit.

---

## Offline Support

A future version could persist the latest arrival data locally.

Potential architecture:

```text
Remote API
    │
    ▼
Repository
    │
    ├── Remote data
    │
    └── Local cache
```

This would provide stronger offline behaviour.

---

## Notifications

FCM could eventually be used to implement:

```text
Bus 27 arriving in approximately 5 minutes
```

for a selected favourite.

Other potential notifications:

```text
Service disruption
Line change
Stop disruption
Favourite route update
```

---

## Monitoring

The backend could eventually expose health information:

```text
GET /health
```

and potentially metrics such as:

* API request count
* EMT response latency
* Error rate
* Average response time
* Number of active requests

---

## CI/CD

GitHub Actions could be introduced to automatically execute:

```text
Push
 │
 ▼
GitHub Actions
 │
 ├── Python tests
 ├── Lint
 ├── Android build
 └── Validation
```

Potential pipeline:

```text
pytest
ruff
Gradle tests
Gradle build
```

---

## Code Quality

Future improvements could include:

* Ruff configuration
* Kotlin linting
* Static analysis
* Formatting checks
* Test coverage reporting
* Pre-commit hooks

---

## Docker

The Python backend could be containerized:

```text
Docker
   │
   └── FastAPI
```

This would simplify local development and deployment.

---

## Production Deployment

The backend could eventually be deployed to a cloud environment.

Possible architecture:

```text
Android
   │
   ▼
HTTPS
   │
   ▼
Cloud Load Balancer
   │
   ▼
FastAPI
   │
   ▼
EMT API
```

---

# Engineering Decisions

## Why a Backend?

The Android application could theoretically communicate directly with the external EMT API.

However, introducing a backend provides several advantages:

* Centralized API integration.
* Separation between mobile and external services.
* Easier credential management.
* Ability to add business logic.
* Easier testing.
* Possibility of supporting additional clients.
* Easier future monitoring and observability.

---

## Why Jetpack Compose?

Jetpack Compose provides a declarative UI model.

Instead of manually modifying UI components, the application represents the UI as a function of application state.

For example:

```text
State changes
     │
     ▼
Compose recomposition
     │
     ▼
Updated UI
```

This is particularly useful for an application containing:

* Loading states
* Errors
* Dynamic arrival lists
* Favourite state
* Connection status

---

## Why SharedPreferences for Favorites?

The current application only needs to persist a small amount of simple data:

```text
stopId
line
destination
```

`SharedPreferences` is therefore sufficient for the current proof of concept.

If the application evolves to support:

* Multiple favourites
* Larger datasets
* Arrival history
* Offline caching

then a database such as Room would be more appropriate.

---

## Why Keep Previous Data on Connection Failure?

Real-time transport information is useful even if the latest request fails.

Removing all previous information would provide a worse experience.

Instead:

```text
Previous data
+
Connection warning
+
Last successful update
```

allows the user to make an informed decision about the freshness of the displayed information.

---

# Learning Objectives

This project was designed to provide hands-on experience with several areas of modern software engineering.

## Backend Engineering

* Python application structure
* REST API development
* FastAPI
* HTTP clients
* Authentication
* Data modelling
* Error handling
* Testing

---

## Android Engineering

* Kotlin
* Android Studio
* Jetpack Compose
* Reactive state
* Coroutines
* Retrofit
* Local persistence
* Firebase Cloud Messaging

---

## Integration Engineering

The project integrates multiple independent systems:

```text
EMT API
   +
Python
   +
FastAPI
   +
Retrofit
   +
Android
   +
Firebase
```

This makes the project representative of a real integration-oriented application rather than an isolated coding exercise.

---

# Example User Journey

A typical user session looks like this:

### 1. Open application

```text
EMT Real-Time
```

### 2. Enter stop

```text
Número de parada: 72
```

### 3. Optionally select line

```text
Línea: 27
```

### 4. Search

```text
Buscar
```

### 5. Results

```text
Línea 27       4 min
Plaza Castilla
850 m

Línea 27       12 min
Plaza Castilla
1.7 km
```

### 6. Save favourite

```text
⭐ Guardar como favorito
```

### 7. Next session

The favourite appears automatically.

Selecting it restores:

```text
Stop = 72
Line = 27
```

and performs a new search.

---

# Example Architecture

The complete system can be represented as:

```text
                         ┌───────────────────────┐
                         │       User            │
                         └───────────┬───────────┘
                                     │
                                     ▼
                         ┌───────────────────────┐
                         │   Android Application │
                         │                       │
                         │ Kotlin                │
                         │ Jetpack Compose       │
                         │ Retrofit              │
                         │ SharedPreferences     │
                         │ Firebase Messaging    │
                         └───────────┬───────────┘
                                     │
                                  HTTP
                                     │
                                     ▼
                         ┌───────────────────────┐
                         │     FastAPI Backend   │
                         │                       │
                         │ API                   │
                         │ Arrival Service       │
                         │ EMT Client            │
                         │ Data Models           │
                         └───────────┬───────────┘
                                     │
                                  HTTPS
                                     │
                                     ▼
                         ┌───────────────────────┐
                         │      EMT Madrid       │
                         │    Mobility Labs API  │
                         └───────────────────────┘
```

---

# Roadmap

The project roadmap is currently focused on moving the application from proof of concept toward a more production-oriented architecture.

```text
[x] EMT API integration
[x] Python backend
[x] FastAPI
[x] Android application
[x] Jetpack Compose
[x] Bus stop search
[x] Line filtering
[x] Favorites
[x] Automatic refresh
[x] Connection fallback
[x] Firebase integration
[x] GitHub repository

[ ] Multiple favorites
[ ] ViewModel
[ ] Repository pattern
[ ] Explicit UI state
[ ] Offline cache
[ ] More comprehensive tests
[ ] CI/CD
[ ] Backend Docker image
[ ] Monitoring / metrics
[ ] Production deployment
[ ] Arrival notifications
```

---

# Project Goals

The long-term goal is to evolve the current proof of concept into a robust real-time transport application demonstrating:

* Backend engineering
* Mobile development
* API integration
* Data modelling
* Asynchronous programming
* Error handling
* Local persistence
* Automated testing
* CI/CD
* Cloud deployment
* Observability

The project is intentionally being developed incrementally, with working functionality validated before introducing additional architectural complexity.

---

# License

This project is currently a personal portfolio.

License information can be added here when the project is released under a specific open-source license.
