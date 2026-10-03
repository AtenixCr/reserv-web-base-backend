IF DATABASE_PRINCIPAL_ID(N'tourism_app_local') IS NOT NULL
    GRANT DELETE ON dbo.ExchangeRate TO tourism_app_local;
GO
