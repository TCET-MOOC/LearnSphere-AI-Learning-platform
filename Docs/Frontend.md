# LearnSphere Frontend Documentation

## Overview

The frontend of LearnSphere is built using **Angular (v17+)**. It provides a single-page application (SPA) experience with distinct dashboards for Students, Teachers, and Admins.

## Project Structure

The codebase is located in `learnSphere/frontend/learnsphere/src/app` and follows a feature-based modular structure:

```
src/app/
├── core/       # Singleton services, guards, interceptors, and models
├── features/   # Feature modules/components (auth, student, teacher, admin, landing)
├── shared/     # Reusable UI components (buttons, sidebars, topbars, data tables)
├── store/      # NgRx state management (actions, reducers, selectors, effects)
├── app.routes.ts # Main application routing configuration
└── app.ts        # Main application component
```

## Key Technologies

- **Angular**: Framework for building the SPA.
- **NgRx**: State management for user sessions and shared data.
- **Angular Material** (or Custom CSS/SCSS): For UI components.
- **HLS.js**: Used for parsing and playing HTTP Live Streaming (HLS) video content for lectures.
- **Razorpay SDK**: Integrated for the payment checkout flow (currently in a simulated/sandbox configuration).

## Features

- **Role-Based Routing**: Angular router is protected by `authGuard` and `roleGuard` to prevent users from accessing unauthorized dashboards.
- **Responsive UI**: Custom SCSS provides a fluid experience across desktop and mobile viewing.
- **Distraction-Free Video Player**: Custom video player component with timestamp bookmarking and integrated note-taking.
