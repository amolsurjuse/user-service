# TeamCity pipeline setup

This folder contains an idempotent script to provision the `user-service` TeamCity pipeline using the TeamCity REST API.

## Prerequisites
- TeamCity server running at `http://localhost:8111`
- Super-user token or personal access token
- `jq` installed

## Usage
```bash
cd /Users/amolsurjuse/development/projects/user-service
TEAMCITY_TOKEN='<token>' ./ci/teamcity/setup_pipeline.sh
```

## Optional environment overrides
- `TEAMCITY_URL` default `http://localhost:8111`
- `TEAMCITY_PARENT_PROJECT_ID` default `Amy`
- `TEAMCITY_PROJECT_ID` default `Amy_UserService`
- `TEAMCITY_BUILD_TYPE_ID` default `Amy_UserService_Build`
- `USER_SERVICE_GIT_URL` default `https://github.com/amolsurjuse/user-service`
- `USER_SERVICE_GIT_BRANCH` default `main`
- `USER_SERVICE_DOCKER_IMAGE` default `amolsurjuse/user-service`
