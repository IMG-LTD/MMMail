<div align="center">

# MMMail - All-in-One Collaborative Office Platform

<img src="https://file.znote.cn/gost/local/%E6%9D%82%E4%B8%83%E6%9D%82%E5%85%AB/logo%20(1).png" width="150" alt="MMMail Logo"/>

</div>

<div align="center">

![License](https://img.shields.io/badge/license-MIT-green.svg)
![Framework](https://img.shields.io/badge/Framework-Spring%20Boot-green.svg)
![ORM](https://img.shields.io/badge/ORM-MyBatis-blue.svg)
![Based on](https://img.shields.io/badge/Based%20on-SmartAdmin-blue.svg)
![WeChat](https://img.shields.io/badge/WeChat-Official%20Account-brightgreen.svg)

</div>

**MMMail** is a future-oriented, enterprise-grade collaborative office platform designed to break down communication barriers and enhance team efficiency. By integrating core features like instant messaging, document collaboration, email management, and task tracking, and providing seamless integration with mainstream office applications such as WeChat Work and DingTalk, we create an efficient, convenient, and customizable one-stop work portal for businesses.

**[Official Website](https://www.mmmail.com) | [Live Demo](https://demo.mmmail.com)**

---

## ✨ Core Features

MMMail platform is planned to offer the following powerful features to meet the diverse needs of modern enterprises:

- **🚀 Instant Messaging (Online Chat)**
  - Supports one-on-one private chats and multi-person group discussions.
  - Send files, images, and emojis to make communication lively and efficient.
  - Cloud-synced message history for easy access to important information anytime, anywhere.

- **📄 Document Collaboration (Docs & Sheets)**
  - Online preview for various document formats (Word, Excel, PDF).
  - Real-time multi-person online editing with clear revision tracking and version history.
  - Robust permission management to ensure the security of corporate knowledge assets.

- **✉️ Email Management (Mail)**
  - Integrate corporate email for unified reception, sending, archiving, and searching.
  - Smart categorization and tagging to help users quickly process large volumes of email.

- **✅ Task Management (Tasks)**
  - Create, assign, and track team tasks for a clear overview of project progress.
  - Set task priorities and deadlines to ensure important work is completed on time.
  - Link with chat and document features for efficient task-centered collaboration.

- **🔌 Ecosystem Integrations**
  - Seamlessly connect with **WeChat Work** and **DingTalk** to synchronize organizational structures and notifications.
  - Support for integration with other internal enterprise systems (e.g., ERP, CRM) via open APIs.

- **🏢 Industry Customization**
  - The platform features a modular design, allowing businesses to select and customize functional modules based on their industry needs (e.g., sales, software development, manufacturing), creating the most suitable collaborative tool.

---

## 🗺️ Roadmap

The project is currently in its initial phase. We will plan, develop, and launch features according to the following timeline.

#### **Phase 1: Q3 2025 - Laying the Foundation**
- [ ] **Goal**: Finalize technology stack, architectural design, and set up core infrastructure services.
- [ ] **Key Tasks**:
    - Establish user authentication and permission management system (RBAC).
    - Build the basic infrastructure for instant messaging and email services.
    - Design and implement the core database model.

#### **Phase 2: Q4 2025 - MVP (Minimum Viable Product)**
- [ ] **Goal**: Release the first viable version with the most essential collaboration features.
- [ ] **Key Tasks**:
    - **Chat**: Launch private chat, group chat, and file transfer.
    - **Mail**: Implement basic email sending, receiving, and management.
    - **Tasks**: Provide task creation, assignment, and status tracking.
    - **Docs**: Support document uploading and online preview.

#### **Phase 3: Q1 2026 - Enhancing Collaboration**
- [ ] **Goal**: Deepen core functionalities and improve the collaborative experience.
- [ ] **Key Tasks**:
    - **Docs**: Introduce real-time multi-person collaborative editing.
    - **Tasks**: Add sub-tasks, deadline reminders, and project boards (Kanban).
    - **Integrations**: Initial integration with WeChat Work and DingTalk for message notifications.

#### **Phase 4: Q2 2026 & Beyond - Building the Platform Ecosystem**
- [ ] **Goal**: Fully integrate third-party applications and launch industry-specific solutions.
- [ ] **Key Tasks**:
    - **Integrations**: Achieve deep integration with WeChat Work and DingTalk (e.g., in-app single sign-on).
    - **Customization**: Release tailored feature modules for industries like sales and software development.
    - **Expansion**: Explore advanced features such as video conferencing and intelligent approval workflows.

---

## 🛠️ Tech Stack (Tentative)

- **Backend**: Spring Boot, Spring Cloud, WebSocket, MyBatis-Plus, Sa-Token
- **Frontend**: Vue.js, Element Plus / Ant Design Vue
- **Database**: MySQL, Redis
- **Middleware**: RabbitMQ / Kafka
- **Deployment**: Docker, Kubernetes

---

## 🚀 Getting Started

> The project is under intensive development. Stay tuned!

### Local Development Setup

To run the project successfully, you need to configure credentials for your local development environment. For security reasons, these credentials are not included in the repository.

1.  Locate the development configuration file: `m-parent/m-base/src/main/resources/dev/m-base.yaml`.
2.  In this file, fill in your local environment's credentials:
    -   `spring.datasource.password`: Your local database password.
    -   `spring.datasource.druid.password`: The password you set for Druid monitoring.
    -   `spring.mail.password`: Your mail server password.

Once configured, you can start the application.

---

## ❤️ Contributing

We welcome all developers interested in the MMMail project to join us!

> Contribution guidelines will be published once the project reaches a stable phase.

---

## 🙏 Acknowledgements

This project is developed based on the excellent open-source project **SmartAdmin**, created by the **1024 Innovation Lab in Luoyang, China**. We express our sincere gratitude to the SmartAdmin team for their selfless dedication and outstanding work.

- **SmartAdmin Official Docs**: [https://smartadmin.vip](https://smartadmin.vip)
- **1024 Innovation Lab**: [https://www.1024lab.net](https://www.1024lab.net)
