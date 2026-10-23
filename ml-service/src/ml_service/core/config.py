from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "WhoDis ML Service"
    app_version: str = "0.1.0"
    debug: bool = False

    model_config = SettingsConfigDict(
        env_prefix="WHODIS_",
        env_file=".env",
        extra="ignore",
    )


settings = Settings()