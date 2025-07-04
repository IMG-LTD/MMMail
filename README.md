<div align="center">

# MMMail - 一站式协同办公平台

<img src="https://file.znote.cn/gost/local/%E6%9D%82%E4%B8%83%E6%9D%82%E5%85%AB/logo%20(1).png" width="150" alt="MMMail Logo"/>

</div>

<div align="center">

![License](https://img.shields.io/badge/license-MIT-green.svg)
![Framework](https://img.shields.io/badge/Framework-Spring%20Boot-green.svg)
![ORM](https://img.shields.io/badge/ORM-MyBatis-blue.svg)
![Based on](https://img.shields.io/badge/Based%20on-SmartAdmin-blue.svg)
![WeChat](https://img.shields.io/badge/WeChat-Official%20Account-brightgreen.svg)

</div>

**MMMail** 是一款面向未来的企业级协同办公平台，旨在打破沟通壁垒，提升团队协作效率。我们通过整合即时通讯、文档协作、邮件管理和任务跟进等核心功能，并提供与企业微信、钉钉等主流办公应用的无缝集成，为企业打造一个高效、便捷、可定制的一站式工作入口。

**[官网](https://www.mmmail.com) | [Demo 示例](https://demo.mmmail.com)**

---

## ✨ 核心功能

MMMail 平台计划提供以下强大功能，以满足现代企业的多元化需求：

- **🚀 即时通讯 (Online Chat)**
  - 支持一对一私聊、多人组群讨论。
  - 文件、图片、表情包收发，让沟通生动高效。
  - 消息历史记录云端漫游，随时随地查找重要信息。

- **📄 文档协作 (Docs & Sheets)**
  - 支持多种格式文档（Word, Excel, PDF）的在线预览。
  - 实现多人实时在线编辑，修改痕迹清晰可见，版本可追溯。
  - 强大的权限管理，确保企业知识资产安全可控。

- **✉️ 邮件管理 (Mail)**
  - 整合企业邮箱，实现邮件的统一收发、归档和搜索。
  - 智能分类和标签，帮助用户快速处理海量邮件。

- **✅ 任务管理 (Tasks)**
  - 创建、指派、跟进团队任务，项目进度一目了然。
  - 设置任务优先级和截止日期，确保重要工作按时完成。
  - 与聊天、文档功能联动，围绕任务进行高效协作。

- **🔌 生态集成 (Integrations)**
  - 无缝接入**企业微信**和**钉钉**，同步组织架构和消息通知。
  - 支持通过开放API与其他企业内部系统（如ERP, CRM）集成。

- **🏢 行业定制 (Customization)**
  - 平台采用模块化设计，企业可根据自身行业特点（如销售、软件开发、生产制造等）选择和定制所需的功能模块，打造最适合自己的协同工具。

---

## 🗺️ 项目路线图 (Roadmap)

项目目前处于起步阶段，我们将按照以下时间线进行功能的规划、开发和上线。

#### **第一阶段：Q3 2025 - 奠定基础**
- [ ] **目标**：完成项目技术选型、架构设计，并搭建核心基础服务。
- [ ] **关键任务**：
    - 建立用户认证与权限管理体系 (RBAC)。
    - 完成即时通讯和邮件服务的基础架构搭建。
    - 设计并实现数据库核心模型。

#### **第二阶段：Q4 2025 - MVP (最小可行产品)**
- [ ] **目标**：发布第一个可用版本，包含最核心的协作功能。
- [ ] **关键任务**：
    - **聊天**：上线私聊、群聊和文件传输功能。
    - **邮件**：实现基本的邮件收发和管理。
    - **任务**：提供任务的创建、分配和状态跟踪。
    - **文档**：支持文档上传和在线预览。

#### **第三阶段：Q1 2026 - 增强协作**
- [ ] **目标**：深化核心功能，提升协作体验。
- [ ] **关键任务**：
    - **文档**：引入多人实时协同编辑功能。
    - **任务**：增加子任务、截止日期提醒和项目看板。
    - **集成**：初步接入企业微信和钉钉，实现消息通知。

#### **第四阶段：Q2 2026 及以后 - 打造平台生态**
- [ ] **目标**：全面集成第三方应用，并推出行业解决方案。
- [ ] **关键任务**：
    - **集成**：实现企业微信、钉钉应用的深度集成（如应用内免登）。
    - **定制**：发布针对销售、软件等行业的定制功能模块。
    - **扩展**：探索视频会议、智能审批流等高级功能。

---

## 🛠️ 技术栈 (Tentative)

- **后端**: Spring Boot, Spring Cloud, WebSocket, MyBatis-Plus, Sa-Token
- **前端**: Vue.js, Element Plus / Ant Design Vue
- **数据库**: MySQL, Redis
- **中间件**: RabbitMQ / Kafka
- **部署**: Docker, Kubernetes

---

## 🚀 如何开始 (Getting Started)

> 项目正在紧张开发中，敬请期待！

### 本地开发配置

为了项目能够成功运行，您需要配置一些本地开发环境的凭证。由于安全原因，这些凭证并未包含在代码仓库中。

1.  找到开发环境的配置文件：`m-parent/m-base/src/main/resources/dev/m-base.yaml`。
2.  在该文件中填入您本地环境的凭证信息：
    -   `spring.datasource.password`: 您的本地数据库密码。
    -   `spring.datasource.druid.password`: 您为 Druid 监控设置的密码。
    -   `spring.mail.password`: 您的邮件服务器密码。

配置完成后，即可启动项目。

---
## ❤️ 贡献 (Contributing)

我们欢迎所有对 MMMail 项目感兴趣的开发者加入我们！

> 贡献指南将在项目进入稳定阶段后发布。

---

## 🙏 致谢 (Acknowledgements)

本项目的开发基于优秀的开源项目 **SmartAdmin**，由 **中国·洛阳 1024创新实验室** 倾力打造。在此，我们对 SmartAdmin 团队的无私奉献和卓越工作表示衷心的感谢！

- **SmartAdmin 官方文档**: [https://smartadmin.vip](https://smartadmin.vip)
- **1024创新实验室**: [https://www.1024lab.net](https://www.1024lab.net)
