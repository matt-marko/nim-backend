# nim-backend

This project was created using the [Quarkus](https://www.quarkus.io) framework. The code for the frontend can be found in the [nim-angular](https://www.github.com/matt-marko/nim-angular) repository.

## Links 

Link to application: https://nim-angular.netlify.app/

## Information

This is the backend component of the nim game, which handles the saving of high scores by exposing a REST API and persisting high scores in a PostgreSQL database.

The backend also includes a WebSocket server to let users play the game online. To save on hosting time, the connections to the server will automatically close after two minutes of inactivity.

## Local Setup

More information may be found about Quarkus and the steps for running the project locally in [quarkus-readme.md](quarkus-readme.md).
