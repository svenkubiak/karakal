Karakal
================

Karakal is a self-hosted user management and authentication server that uses the WebAuthn standard to provide secure, passwordless authentication as an identity provider in front of existing applications.

### How it works

In Karakal’s Dashboard you configure an App, which holds the metadata required for integrating with your application. Using this metadata, you can generate a login page from the provided code snippets.

When a user accesses your application, check if a JWT is present and can be verified with your application’s public key. If no valid token is found, redirect the user to the Karakal login page. Once the login flow completes successfully, Karakal sets a cookie and redirects the user to the callback URL you defined. At that point, validate the JWT again and continue handling the request within your application using the verified identity.

### Prerequisites

Before starting the installation process, make sure you have the following prerequisites:

- **Docker**: Ensure Docker is installed and running on your system
- **Docker Compose**: Make sure Docker Compose is installed to manage multi-container applications
- **Web Frontend Server**: A frontend HTTP server (e.g. Nginx) to handle SSL termination and proxy requests to the backend.

### Installation

1. **Create the directory for your server installation:**

   First, create a folder where you want to install your server. For this example, we will use the folder name `karakal`.

   ```shell
   mkdir karakal
   cd karakal
   ```
2. **Download and execute the installation script:**

   ```shell
   curl -sSL https://raw.githubusercontent.com/svenkubiak/karakal/refs/heads/main/install.sh | bash
   ```

3. **After installation is complete open the .env file and set your custom configuration:**