# cantstopthesignal

# Development Note
This will be intended to be used through the I2P network and I do plan to make at least some pluggable values such as MOTD, website name etc so others can use my work to host their own personal forum websites in a safe, secure manner. I2P is ideal for this due to the architecture of the network protocol itself, Tor CAN be used however without TLS the final node will see the plaintext, now will they know what it's for if it's just a random username and password? Probably not, but if anything in there is identifiable or you use your name or something you use elsewhere along with a password you use elsewhere, that is no-bueno. 

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:
 * [Ktor Documentation](https://ktor.io/docs/home.html)
 * [Ktor GitHub page](https://github.com/ktorio/ktor)
 * [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). [Request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).


## Features
Here's a list of features included in this project:

| Name | Description |
|------|-------------|
| [Rate Limiting](https://start.ktor.io/p/io.github.flaxoos/server-rate-limiting) | Manage request rate limiting as you see fit |
| [Thymeleaf](https://start.ktor.io/p/io.ktor/server-thymeleaf) | Serves HTML content, templated using Thymeleaf |
| [Content Negotiation](https://start.ktor.io/p/io.ktor/server-content-negotiation) | Provides automatic content conversion according to Content-Type and Accept headers |
| [Jackson](https://start.ktor.io/p/io.ktor/server-jackson) | Handles JSON serialization using Jackson library |
| [Status Pages](https://start.ktor.io/p/io.ktor/server-status-pages) | Provides exception handling for routes |
| [Authentication](https://start.ktor.io/p/io.ktor/server-auth) | Provides extension point for handling the Authorization header |
| [Authentication JWT](https://start.ktor.io/p/io.ktor/server-auth-jwt) | Handles JSON Web Token (JWT) bearer authentication scheme |
| [Authentication Basic](https://start.ktor.io/p/io.ktor/server-auth-basic) | Handles 'Basic' username / password authentication scheme |
| [Default Headers](https://start.ktor.io/p/io.ktor/server-default-headers) | Adds a default set of headers to HTTP responses |


## Building & Running
To build or run the project, use one of the following tasks:


| Task | Description |
|------|-------------|

If the server starts successfully, you'll see the following output:
```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```
