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

Conceitos reativos presentes no projeto
--------------------------------------

Este projeto foi pensado como um tutorial e contém (e demonstra) os principais conceitos reativos listados abaixo. A seguir explico cada conceito, como ele aparece no código e onde encontrá-lo.

1) Mono vs Flux
- Mono<T> representa 0..1 elemento; Flux<T> representa 0..N elementos.
- Onde no projeto:
  - `UserController.create` recebe `Mono<UserRequest>` e retorna `Mono<ResponseEntity<UserResponse>>` (fluxo de 0..1) — veja `src/main/java/com/br/webfluxreativo/controller/UserController.java` linhas ~49-56.
  - `UserController.getAll` retorna `Flux<UserResponse>` obtido diretamente de `service.findAll()` (0..N) — linhas ~35-39.
  - `UserService` usa `Mono` e `Flux` em seus métodos (`findAll(): Flux`, `create(): Mono`, etc.) — `src/main/java/com/br/webfluxreativo/service/UserService.java`.

2) Backpressure
- Backpressure é a forma de controlar o fluxo quando o consumidor não consegue processar os dados tão rápido quanto o produtor. Reactor suporta operadores de controle de demanda (request) e operadores como `onBackpressureBuffer`, `onBackpressureDrop`, e `limitRate`.
- Onde no projeto:
  - O exemplo básico não contém um pipeline explícito de backpressure (por simplicidade), mas como o `Flux` é consumido reativamente pelo servidor (Netty) e pelos clientes reativos, a infraestrutura respeita sinais de demanda.
  - Para demonstrar na prática, você pode alterar `UserController.getAll()` para aplicar `limitRate` ou `onBackpressureBuffer` sobre o `Flux` retornado:

```java
// exemplo: limitar a taxa para consumo de 10 elementos por vez
return service.findAll().limitRate(10);
```

3) Reactive Streams
- Reactive Streams é a especificação (Publisher, Subscriber, Subscription, Processor) adotada pelo Reactor e implementada pelo Spring WebFlux.
- Onde no projeto:
  - `Flux` e `Mono` são implementações do `org.reactivestreams.Publisher` usadas em controllers, services e repositórios. O `ReactiveMongoRepository` retorna publishers reativos que obedecem ao contrato (demand/signals).

4) Non-blocking I/O
- Spring WebFlux, por padrão, usa o servidor Netty (não-bloqueante) quando detectado no classpath. Isso permite que poucas threads (event loop) sirvam muitas conexões, desde que não ocorram operações bloqueantes dentro das pipelines.
- Onde no projeto:
  - A aplicação é reativa end-to-end: controllers usam tipos reativos, service e repository também. Evite chamadas síncronas (bloqueantes) dentro dos métodos do `Service` (por exemplo, não chame APIs HTTP com clientes bloqueantes ou DBs não reativos sem `.subscribeOn(Schedulers.boundedElastic())`).

5) Integração com MongoDB Reativo
- Usamos `ReactiveMongoRepository<UserEntity, String>` (`UserRepository`) que retorna `Flux`/`Mono` e funciona com o driver reativo do MongoDB.
- Onde no projeto:
  - `src/main/java/com/br/webfluxreativo/repository/UserRepository.java`
  - `src/main/java/com/br/webfluxreativo/entity/UserEntity.java`
  - As operações `repository.findAll()`, `repository.save(...)`, `repository.findById(...)` são reativas e não bloqueantes.

6) Fluxo assíncrono de ponta a ponta
- O projeto demonstra um fluxo assíncrono completo:
  - Cliente HTTP → Controller (recebe Mono/Flux) → Service (transformações, validações) → Repository (operações reativas no Mongo) → Controller (mapeia para ResponseEntity) → Cliente.
- Exemplo de fluxo (criar um usuário):

```text
Cliente POST /users (JSON) --> Controller recebe Mono<UserRequest>
  flatMap para service.create(UserRequest) --> Service converte DTO para entidade
  repository.save(entity) retorna Mono<UserEntity>
  map para UserResponse --> ResponseEntity criado e retornado
```

Onde ver no código:
  - Controller: `src/main/java/com/br/webfluxreativo/controller/UserController.java`
  - Service: `src/main/java/com/br/webfluxreativo/service/UserService.java`
  - Repository: `src/main/java/com/br/webfluxreativo/repository/UserRepository.java`

Dicas práticas para experimentar estes conceitos
- Teste Mono vs Flux: crie endpoints de streaming (Server-Sent Events) ou retorne grandes listas via `Flux` e experimente `limitRate`/`onBackpressureBuffer`.
- Simule backpressure usando um cliente que procesa lentamente (ex.: um consumidor que lê com delays) e observe a taxa de emissão.
- Garanta que nenhuma operação bloqueante seja chamada no pipeline reativo; se necessário, isole chamadas bloqueantes em `Schedulers.boundedElastic()`.

Se quiser, eu adiciono exemplos práticos (pequenas alterações no código) que demonstram backpressure ativo e um endpoint streaming (SSE) para ver o comportamento em tempo real. Quer que eu implemente isso no projeto agora? 

