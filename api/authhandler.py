from flask import Flask, Response
from authlib.integrations.flask_client import OAuth
from datetime import datetime, timedelta, timezone
from typing import Any
import jwt

class Authenticator:
    def __init__(self, app: Flask, clientId: str, clientSecret: str):
        self.oauth = OAuth(app)
        self.google = self.oauth.register(
            name="google",
            client_id=clientId,
            client_secret=clientSecret,
            server_metadata_url="https://accounts.google.com/.well-known/openid-configuration",
            client_kwargs={"scope": "openid email profile"}
        )

    def googleRedirect(self, url: str) -> Response:
        return self.google.authorize_redirect(url)

    def googleToken(self) -> dict[str, Any]:
        return self.google.authorize_access_token()

def createJwt(userInfo: dict, jwtSecret: str) -> str:
    return jwt.encode(
        {
            "sub": userInfo["sub"],
            "email": userInfo["email"],
            "exp": datetime.now(timezone.utc) + timedelta(days=30)
        },
        jwtSecret,
        algorithm="HS256"
    )
