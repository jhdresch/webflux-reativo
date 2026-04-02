# Webflux Reativo — Projeto Tutorial

Este repositório é um projeto tutorial em Java usando Spring WebFlux e MongoDB reativo. O objetivo é servir como exemplo prático de uma API REST não-bloqueante com operadores reativos, repositórios reativos e integração via Docker.

Conteúdo deste README
- Visão geral
- Pré-requisitos
- Como rodar (local / Docker)
- Variáveis de ambiente (.env)
- Endpoints (exemplos com curl)
- Modelos (DTOs / entidade)
- Como o WebFlux funciona — resumo e métodos principais
- Testes
- Troubleshooting comum

---

Visão geral
---------

O projeto expõe um recurso `User` com operações CRUD reativas:

- GET /users — lista todos os usuários (Flux<UserResponse>)
- GET /users/{id} — busca usuário por id (Mono<ResponseEntity<UserResponse>>)
- POST /users — cria um usuário (recebe Mono<UserRequest>)
- PUT /users/{id} — atualiza usuário
- DELETE /users/{id} — deleta usuário

O backend persiste em MongoDB usando `ReactiveMongoRepository` e mapeamentos entre `UserRequest`, `UserEntity` e `UserResponse`.

Pré-requisitos
--------------

- Java 17
- Maven 3 (ou usar o wrapper `mvnw` incluído)
- Docker & Docker Compose (se for rodar via containers)

Executando localmente (sem Docker)
-------------------------------

1. Build e testes:

```powershell
.\mvnw.cmd clean package
```

2. Rodar a aplicação (após configurar um Mongo disponível):

```powershell
.\mvnw.cmd spring-boot:run
```

Observação: por padrão o projeto lê `spring.data.mongodb.uri` do `application.yml`/.env. Se estiver rodando localmente sem Docker, aponte a variável `SPRING_DATA_MONGODB_URI` para seu Mongo (ex.: `mongodb://localhost:27017/webflux_reativo`).

Executando com Docker Compose
----------------------------

O repositório contém um `docker-compose.yml` e um script de inicialização `docker/mongo-init/init.js`.

1. Verifique/edite `.env` com as credenciais e portas desejadas.

2. Suba os serviços:

```powershell
docker-compose up --build
```

3. Logs úteis:

```powershell
docker-compose logs -f mongo
docker-compose logs -f app
```

Importante: o script de init só é executado na primeira inicialização do banco (quando `/data/db` está vazio). Para forçar reexecução apague os dados em `./data/mongo` (atenção: apaga dados).

Variáveis de ambiente (.env) — exemplo
------------------------------------

O projeto inclui um `.env` de exemplo com as seguintes variáveis relevantes:

- APP_PORT — porta da aplicação (ex.: 8091)
- MONGO_INITDB_ROOT_USERNAME / MONGO_INITDB_ROOT_PASSWORD — credenciais root do Mongo
- MONGO_INITDB_DATABASE — nome do banco de dados da aplicação
- MONGO_EXTERNAL_PORT — porta exposta no host (host -> container)
- APP_MONGO_USER / APP_MONGO_PASSWORD — usuário da aplicação criado pelo init script
- SPRING_DATA_MONGODB_URI — string de conexão usada pela app dentro do Docker

Exemplo (presente no repo `.env`):

```
APP_PORT=8091
MONGO_INITDB_ROOT_USERNAME=rootadmin
MONGO_INITDB_ROOT_PASSWORD=rootpassword123
MONGO_INITDB_DATABASE=webflux_reativo
MONGO_EXTERNAL_PORT=27018
APP_MONGO_USER=appuser
APP_MONGO_PASSWORD=apppassword
SPRING_DATA_MONGODB_URI=mongodb://appuser:apppassword@mongo:27017/webflux_reativo?authSource=admin
```

Endpoints e exemplos
--------------------

Base URL: http://localhost:${APP_PORT}

1) Listar usuários

GET /users

curl:

```bash
curl -s http://localhost:8091/users
```

Retorna um array (Flux) de objetos `UserResponse`:

```json
[ { "id": "...", "name": "João", "email": "joao@ex.com" } ]
```

2) Buscar por id

GET /users/{id}

```bash
curl -i http://localhost:8091/users/6412c0...
```

3) Criar usuário

POST /users

Request JSON (UserRequest):

```json
{
  "name": "Maria",
  "email": "maria@ex.com",
  "password": "senha123"
}
```

curl:

```bash
curl -X POST -H "Content-Type: application/json" -d '{"name":"Maria","email":"maria@ex.com","password":"senha123"}' http://localhost:8091/users
```

Resposta: 201 Created com o `UserResponse` e header `Location: /users/{id}`.

4) Atualizar usuário

PUT /users/{id}

Envie um JSON `UserRequest` no corpo. Se o usuário existir, retorna 200 OK com o recurso atualizado; senão 404.

5) Deletar usuário

DELETE /users/{id}

Retorna 204 No Content se deletado, 404 se não encontrado.

Modelos (resumo)
----------------

- `UserRequest` (request DTO)
  - name (String) — obrigatório
  - email (String) — obrigatório, formato email
  - password (String) — obrigatório, mínimo 6 caracteres

- `UserResponse` (response DTO)
  - id, name, email

- `UserEntity` (entidade Mongo)
  - id, name, email (único), password

Arquitetura / camadas
---------------------

- Controller (`UserController`) — mapeia endpoints e trabalha com `Mono`/`Flux` no nível HTTP.
- Service (`UserService`) — contém lógica de negócio e transforma DTOs para entidades e vice-versa usando `UserMapper`.
- Repository (`UserRepository`) — estende `ReactiveMongoRepository<UserEntity, String>` — operações reativas com Mongo.

WebFlux — breve explicação e métodos principais
---------------------------------------------

Spring WebFlux é o módulo reativo do Spring para construir aplicações não-bloqueantes. Em WebFlux usamos dois tipos principais:

- Mono<T> — representa 0..1 elemento de forma assíncrona.
- Flux<T> — representa 0..N elementos de forma assíncrona.

Operadores comuns (Reactor)
- map(fn) — transforma cada elemento de forma síncrona
- flatMap(fn que retorna Mono/Flux) — mapeia para publishers assíncronos e os “achata”
- filter(predicate) — filtra elementos
- collectList() — transforma um Flux<T> em Mono<List<T>>
- doOnNext(consumer) — efeito colateral para inspeção
- onErrorResume/fallback — tratamento de erro reativo

Padrões e boas práticas
- Nunca bloqueie dentro de pipelines reativos (evite chamadas síncronas a I/O). Se precisar, executá-las em scheduler apropriado.
- Use tipos reativos nas assinaturas de controllers (ex.: receber `Mono<UserRequest>` para POST).
- Trate erros com operadores reativos para devolver respostas HTTP apropriadas.

Exemplo mínimo (criar usuário) — fluxo típico

Controller recebe `Mono<UserRequest>` → `.flatMap(service::create)` → service faz `mapper.toEntity` → `repository.save(entity)` (retorna Mono<UserEntity>) → `map(mapper::toResponse)` → ResponseEntity

Testes
------

- `mvn test` executa os testes. O projeto tem uma configuração de teste que fornece um `ReactiveMongoTemplate` mock para evitar dependência de um Mongo real durante testes unitários/integração leve.
- Para testes de integração com banco real, adicione `de.flapdoodle.embed.mongo` no scope test (embedded Mongo) e remova a exclusão de auto-config.

Troubleshooting — erros comuns
--------------------------------

1) Erro: required a bean named 'reactiveMongoTemplate' that could not be found
- Causa: aplicação estava excluindo a auto-configuração do Mongo (`@SpringBootApplication(exclude = ...)`) ou nos testes havia `spring.autoconfigure.exclude` — resultado: Spring não criou `ReactiveMongoTemplate`.
- Solução: remover a exclusão do `@SpringBootApplication` no main (ou prover um bean manual) e garantir `spring.data.mongodb.uri` está configurado.

2) AuthenticationFailed / Authentication exception
- Causa: usuário/senha incorretos ou `authSource` diferente do banco onde o usuário foi criado.
- Solução: garantir que o init script crie o usuário no mesmo `authSource` usado pela URI. Ex.: `mongodb://appuser:apppassword@mongo:27017/appdb?authSource=admin` ou criar o usuário em `admin`.

3) `docker-entrypoint-initdb.d` script não rodou
- Lembrete: os scripts em `docker-entrypoint-initdb.d` só correm na primeira inicialização do banco (quando `/data/db` está vazio). Para reexecutar, remova os dados do volume/bind-mount.

Logs e inspeção
---------------

- Ver logs do mongo: `docker-compose logs -f mongo`
- Abrir mongosh no container: `docker exec -it webflux_mongo mongosh --username <root> --password <pwd> --authenticationDatabase admin`
- Listar usuários: `use admin; db.getUsers()`

Contribuindo / próximos passos sugeridos
-------------------------------------

- Converter testes para usar embedded Mongo (Flapdoodle) para integração real em CI
- Implementar autenticação/authorization (JWT)
- Criar documentação OpenAPI / Swagger
- Exemplos de streams reativos e backpressure

Licença
-------

Este projeto é um tutorial — adapte conforme necessário.

---

Se quiser, eu gero também um `README.md` em inglês, ou adiciono um `docker-compose.override.yml` que usa a porta interna 27018 do container conforme discutido anteriormente. Quer que eu gere esses itens adicionais agora? 

