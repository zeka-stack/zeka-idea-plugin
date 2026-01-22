# Project Context: Zeka Idea WebUI

## Overview

This project (`zeka-idea-webui`) is the web-based user interface for the **Zeka Stack**, specifically designed to complement the Zeka
IntelliJ IDEA plugin. It is built using **React 19**, **TypeScript**, and **Vite**, styled with **Tailwind CSS**.

The application serves as a dashboard or companion site, hosting various plugin-related modules such as Changelogs, Engine status, Javadoc,
Repairer tools, and Tracing visualization.

## Architecture & Tech Stack

- **Frontend Framework:** React 19
- **Language:** TypeScript
- **Build Tool:** Vite 7
- **Styling:** Tailwind CSS + PostCSS
- **Icons:** Lucide React
- **Markdown Rendering:** react-markdown + remark-gfm
- **Package Manager:** pnpm

### Directory Structure

- `src/`
    - `pages/`: Top-level route components (Home, FeatureRequests, etc.).
    - `pages/plugins/`: Specific modules for the Zeka plugin features (e.g., `ChangelogHome`, `EngineHome`, `TracerHome`).
    - `components/`: Reusable UI components (Header, Footer, Modals).
    - `lib/`: Utility functions (API clients, authentication).
    - `assets/`: Static assets (images, icons).
- `public/`: Public static assets served directly.
- `deploy.sh`: Bash script for deploying to the production server.
- `zekastack.dong4j.site.conf`: Nginx configuration for the production site.

## Development Workflow

### Prerequisites

- Node.js (Latest LTS recommended)
- pnpm (Package manager)

### Key Commands

| Command        | Description                                                    |
|:---------------|:---------------------------------------------------------------|
| `pnpm install` | Install dependencies.                                          |
| `pnpm dev`     | Start the local development server with HMR.                   |
| `pnpm build`   | Type-check (`tsc`) and build the production bundle to `dist/`. |
| `pnpm lint`    | Run ESLint to check for code quality and style issues.         |
| `pnpm preview` | Preview the production build locally.                          |

## Deployment

The project includes a custom deployment script `deploy.sh` targeting an `aliyun` server.

**Usage:**

- `./deploy.sh`: Builds the project, uploads `dist/` to `/var/www/zeka-idea-webui/dist`, and syncs Nginx config.
- `./deploy.sh -n`: Skips build/upload and only syncs the Nginx configuration.

**Configuration:**

- **Remote Host:** `aliyun` (SSH alias expected).
- **Remote Path:** `/var/www/zeka-idea-webui/dist`.
- **Nginx Config Path:** `/etc/nginx/conf.d`.
- **Live Site:** https://zekastack.dong4j.site

## Conventions

- **Styling:** Use Tailwind CSS utility classes.
- **Components:** Functional components with TypeScript interfaces for props.
- **Routing:** File structure suggests manual routing or a simple router setup in `App.tsx` (verify if `react-router` is used if adding
  routes).
- **State:** React Hooks.
