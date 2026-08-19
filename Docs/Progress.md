# LearnSphere Progress Report

## Current Status: Development Phase

The project has completed its **MVP Boilerplate** and **Structural Scaffolding**. Most of the CRUD operations and UI flows are implemented, but external integrations are pending.

## ✅ Completed (Working)

- **Database Schema**: All 28 entities (Users, Courses, Lectures, Enrollments, Payments, Notes, etc.) are implemented with Spring Data JPA.
- **Authentication**: JWT-based login and registration for Students and Teachers.
- **Frontend UI**: Dashboards for all three roles are fully built and styled.
- **Course Flow**: Teachers can create courses and upload lectures. Students can browse, enroll, and watch lectures.
- **Assessments**: Basic quiz creation and completion logic.

## 🚧 In Progress (Mocked / Stubbed)

- **Payments**: The `CheckoutComponent` and `PaymentController` exist, but Razorpay transactions are currently simulated.
- **Sentiment Analysis**: The `SentimentController` provides a basic keyword-based heuristic instead of real machine learning.
- **Live Sessions**: UI is present, but it currently relies on pasting external Google Meet/Zoom links rather than built-in WebRTC.

## ❌ Pending (Not Started)

- **AI Question Extraction**: No Python/ML service exists to parse video transcripts yet.
- **Rule-Based Chatbot**: Missing from both frontend and backend.
- **Automated At-Risk Detection**: Needs an automated cron job or AI heuristic to flag students.
- **Video Processing (HLS)**: Videos are currently just saved as raw MP4s; an encoder is needed to convert them to HLS streams for adaptive bitrate playback.
