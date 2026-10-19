# KPI RuoYi Adapter

This module is the integration boundary between the KPI core and RuoYi-Vue-Plus. It deliberately contains no assessment rules.

Run `backend/apply-ruoyi-integration.sh` after cloning with submodules. The tracked patch adds the KPI modules to the RuoYi reactor and adds this adapter to `ruoyi-admin`. It can safely be run again; direct edits inside the Git submodule are not stored in the parent repository.

`KpiCurrentActor` gets the current authenticated identity through RuoYi `LoginHelper`; `KpiRuoYiConfiguration` creates the JDBC identity and scope adapters from RuoYi's configured `DataSource`. KPI controllers must obtain their actor through `KpiCurrentActor` and apply `KpiScopeAuthorizationService` before operating on a cycle and unit.
