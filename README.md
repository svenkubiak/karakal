[![Latest](https://img.shields.io/github/v/tag/svenkubiak/karakal?label=ghcr.io&sort=semver)](https://ghcr.io/svenkubiak/filedpapers/filedpapers)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-%F0%9F%8D%BA-yellow)](https://buymeacoffee.com/svenkubiak)

Karakal
================

Karakal is a self-hosted user management and authentication server that uses the WebAuthn standard to provide secure, passwordless authentication as an identity provider in front of existing applications. 🔐

### How it works ⚙️

In Karakal’s Dashboard, you configure an App, which holds the metadata required for integrating with your application. Using this metadata, you can generate a login page from the provided code snippets.

When a user accesses your application, check if a JWT is present and can be verified with your application’s public key. If no valid token is found, redirect the user to the Karakal login page. Once the login flow completes successfully, Karakal sets a cookie and redirects the user to the callback URL you defined. At that point, validate the JWT again and continue handling the request within your application using the verified identity.

### Prerequisites 📝

Before starting the installation process, make sure you have the following prerequisites:

- **Docker**: Ensure Docker is installed and running on your system. 🐳
- **Docker Compose**: Make sure Docker Compose is installed to manage multi-container applications.
- **Web Frontend Server**: A frontend HTTP server (e.g., Nginx) to handle SSL termination and proxy requests to the backend.


### Installation 🚀

1. **Create the directory for your server installation:**

First, create a folder where you want to install your server. For this example, we will use the folder name `karakal`.

```shell
mkdir karakal
cd karakal
```

2. **Download and execute the installation script:**

```shell
curl -fsSL --proto '=https' --tlsv1.2 -O https://raw.githubusercontent.com/svenkubiak/karakal/refs/heads/main/install.sh
less install.sh   # review before running
bash install.sh
```

> 🔒 Piping a remote script straight into a shell (`curl ... | bash`) executes whatever the endpoint returns, without any chance to review it. Download, review, then run. To pin the downloaded `config.yaml` and `compose.yaml` to a released tag instead of `main`, set `KARAKAL_VERSION`, e.g. `KARAKAL_VERSION=1.1.26 bash install.sh`.
>
> The generated `.env` and `config/config.yaml` contain all application secrets and are created with mode `600`.

3. **After installation is complete, open the `.env` file and set your custom configuration:**

```shell
KARAKAL_URL=http://localhost
```

4. **Edit the `docker-compose.yaml` and adjust host and port as needed. Everything else is pre-configured.**
5. **Start up the Docker container:**

```shell
docker compose up -d
```

6. **During the first start-up, the default application called "dashboard" will be set up.**
7. **Configure your Web Frontend Server (e.g., Nginx) to access your Karakal installation.**

### Configuration 🛠️

1. **After setup is completed and your containers are up and running, open your Karakal Dashboard:**

```shell
https://yourdomain.com/dashboard
```

2. **Register an administrative user by clicking on 'Register'. You will be guided through a WebAuthn setup process.**

> ⏱️ The registration of the **first** administrator is unauthenticated and is therefore only possible within **15 minutes** after the application was started. If the window has closed, restart the container (`docker compose restart karakal-web`) to open a new one.

3. **After successful creation of an administrative user, the registration for the default dashboard app is disabled, and you will be redirected to the sign-in page.**
4. **Log in with your registered user. The dashboard will be shown.**
5. **Create a new application and configure it as needed. 🏗️**
