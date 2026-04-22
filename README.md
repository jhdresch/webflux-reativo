# 🚀 WebFlux Reativo — Projeto Tutorial

Este repositório demonstra a construção de um microserviço reativo com Spring WebFlux, utilizando MongoDB reativo, containerização com Docker e execução em Kubernetes. O objetivo é mostrar uma aplicação não‑bloqueante, escalável e resiliente, seguindo boas práticas cloud‑native.

---

## 🧠 Visão Geral

O serviço expõe o recurso `User` com operações CRUD reativas:

- `GET /users` — lista todos os usuários (Flux<UserResponse>)
- `GET /users/{id}` — busca por ID (Mono<ResponseEntity<UserResponse>>)
- `POST /users` — cria usuário (Mono<UserResponse>)
- `PUT /users/{id}` — atualiza usuário (Mono<UserResponse>)
- `DELETE /users/{id}` — remove usuário (Mono<Void>)

---

## 🏗️ Arquitetura

Client  
↓  
Controller (Spring WebFlux)  
↓  
Service (regras de negócio)  
↓  
Repository (ReactiveMongoRepository)  
↓  
MongoDB

Componentes Kubernetes:
- Deployment (Pods com a aplicação WebFlux)
- Service (ClusterIP / LoadBalancer)
- HPA (Horizontal Pod Autoscaler)
- Liveness & Readiness Probes (via Spring Boot Actuator)

---

## ⚙️ Tecnologias Utilizadas

- Java (11/17) + Spring WebFlux
- Spring Data Reactive MongoDB
- Docker / Docker Compose
- Kubernetes + Helm
- CI/CD (GitHub Actions / GitLab CI - sugerido)
- Observability: Actuator, Micrometer, Prometheus/Grafana, Jaeger/Zipkin (opcional)
- Ferramentas de carga: Apache Bench (ab), k6

---

## 🧪 Pré-requisitos

- JDK 11+ (ou 17)
- Maven (ou usar wrapper `./mvnw`)
- Docker & Docker Compose
- kubectl e acesso a cluster Kubernetes (minikube, kind, GKE, EKS, AKS)
- Helm (para deploy com chart)
- MongoDB (local ou em container)

---

## ▶️ Executando Localmente

1) Build:
```bash
./mvnw clean package
```

2) Rodar via Spring Boot:
```bash
./mvnw spring-boot:run
# ou
java -jar target/webflux-reativo-0.0.1-SNAPSHOT.jar
```

3) Variáveis de ambiente:
```bash
export SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/webflux_reativo
export SPRING_PROFILES_ACTIVE=local
```

---

## 🐳 Docker & Docker Compose

Exemplo de `Dockerfile` (coloque na raiz do projeto):
```dockerfile
FROM eclipse-temurin:17-jdk-jammy
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

Exemplo de `docker-compose.yml`:
```yaml
version: '3.8'
services:
  mongo:
    image: mongo:6
    container_name: webflux-mongo
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db

  app:
    build: .
    container_name: webflux-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/webflux_reativo
    depends_on:
      - mongo

volumes:
  mongo-data:
```

---

## ☸️ Kubernetes — Manifests de Exemplo

Crie um diretório `k8s/` e salve os blocos abaixo em arquivos separados.

1) `k8s/deployment.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webflux-app
  labels:
    app: webflux
spec:
  replicas: 2
  selector:
    matchLabels:
      app: webflux
  template:
    metadata:
      labels:
        app: webflux
    spec:
      containers:
        - name: webflux
          image: yourrepo/webflux-app:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_DATA_MONGODB_URI
              value: "mongodb://mongo:27017/webflux_reativo"
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 30
```

2) `k8s/service.yaml`
```yaml
apiVersion: v1
kind: Service
metadata:
  name: webflux-service
spec:
  selector:
    app: webflux
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
  type: ClusterIP
```

---

## 📦 Helm — Chart Básico

### Estrutura recomendada:
```text
helm/
  webflux-chart/
    Chart.yaml
    values.yaml
    templates/
      deployment.yaml
      service.yaml
      hpa.yaml
```

### `helm/webflux-chart/Chart.yaml`
```yaml
apiVersion: v2
name: webflux-chart
version: 0.1.0
description: Helm chart for WebFlux demo
```

### `helm/webflux-chart/values.yaml`
```yaml
image:
  repository: yourrepo/webflux-app
  tag: latest
replicaCount: 2
mongodb:
  uri: "mongodb://mongo:27017/webflux_reativo"
service:
  type: ClusterIP
  port: 80
hpa:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

### Templates (`helm/webflux-chart/templates/`)

#### `deployment.yaml`
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "webflux-chart.fullname" . }}
  labels:
    app: {{ include "webflux-chart.name" . }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      app: {{ include "webflux-chart.name" . }}
  template:
    metadata:
      labels:
        app: {{ include "webflux-chart.name" . }}
    spec:
      containers:
        - name: {{ include "webflux-chart.name" . }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_DATA_MONGODB_URI
              value: "{{ .Values.mongodb.uri }}"
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 30
```

#### `service.yaml`
```yaml
apiVersion: v1
kind: Service
metadata:
  name: {{ include "webflux-chart.fullname" . }}-svc
spec:
  selector:
    app: {{ include "webflux-chart.name" . }}
  ports:
    - protocol: TCP
      port: {{ .Values.service.port }}
      targetPort: 8080
  type: {{ .Values.service.type }}
```

---

## 🛠️ Comandos Úteis

### Kubernetes (direto)
```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

### Helm
```bash
# Instalar o chart
helm install webflux-app ./helm/webflux-chart

# Atualizar o deploy
helm upgrade --install webflux-app ./helm/webflux-chart

# Listar e Desinstalar
helm list
helm uninstall webflux-app
```

---

## 📌 Dependências do projeto (detalhado)

Maven (dependências principais declaradas em `pom.xml`):

- org.springframework.boot:spring-boot-starter-webflux
- org.springframework.boot:spring-boot-starter-actuator
- org.springframework.boot:spring-boot-starter-data-mongodb-reactive
- org.springframework.boot:spring-boot-starter-data-elasticsearch
- org.springframework.boot:spring-boot-starter-validation
- org.projectlombok:lombok (opcional/compile)
- org.mapstruct:mapstruct (+ processor no build)

Dependências de teste:
- org.springframework.boot:spring-boot-starter-test (scope test)
- io.projectreactor:reactor-test (scope test)
- de.flapdoodle.embed:de.flapdoodle.embed.mongo (scope test)

Dependências/integrações opcionais que você pode querer adicionar:
- io.micrometer:micrometer-registry-prometheus — para expor métricas no endpoint `/actuator/prometheus`.

Imagens Docker usadas no `docker-compose.yml` (dev):
- mongo:6.0
- maven:3.8.8-openjdk-17 (para compilar/rodar a aplicação no container em dev)
- eclipse-temurin:17-jre-jammy (runtime da imagem multi-stage)
- docker.elastic.co/elasticsearch/elasticsearch:8.9.3
- docker.elastic.co/kibana/kibana:8.9.3
- curlimages/curl (usado para o job `es-init`)

## 🔧 Variáveis de ambiente (arquivo `.env`)

Principais variáveis (já incluídas no `.env` do projeto):

- APP_PORT — porta exposta da aplicação (ex.: 8091)
- MONGO_INITDB_ROOT_USERNAME / MONGO_INITDB_ROOT_PASSWORD / MONGO_INITDB_DATABASE — credenciais e DB root do Mongo
- MONGO_INTERNAL_PORT / MONGO_EXTERNAL_PORT — porta interna do Mongo no container e porta mapeada no host
- SPRING_DATA_MONGODB_URI — URI que a aplicação usa para conectar ao Mongo (injetada no container app)
- ELASTICSEARCH_INTERNAL_PORT / ELASTICSEARCH_EXTERNAL_PORT
- KIBANA_INTERNAL_PORT / KIBANA_EXTERNAL_PORT
- SPRING_ELASTICSEARCH_URI — URI para Elasticsearch (usada pela app e pelo script init)

Nunca comite credenciais sensíveis em texto plano: use `.gitignore` para `.env` e, para Kubernetes, utilize `Secrets`.

## ▶️ Passo a passo completo para rodar localmente (Docker Compose)

1) Ajuste `.env` conforme necessário.
2) Suba os containers (Mongo, Elasticsearch, Kibana, App e es-init):

```powershell
docker compose up --build
```

3) Verifique logs do `es-init` para confirmar que templates/índices foram criados:

```powershell
docker compose logs -f es-init
```

4) Valide:

```powershell
curl http://localhost:9200/_cat/indices?v
curl http://localhost:5601  # Kibana UI
curl http://localhost:8091/actuator/health
```

Se `es-init` falhar cedo, rode manualmente após ES subir:

```powershell
# aguarde elasticsearch e então:
docker compose run --rm es-init
```

## ▶️ Preparar imagem e rodar em Minikube (Helm)

1) Gerar JAR e construir imagem (local):

```powershell
.\mvnw.cmd -DskipTests package
docker build -t webflux-app:latest .
minikube image load webflux-app:latest
```

2) (Opcional) Crie `Secret` com credenciais do Mongo para usar no Kubernetes:

```powershell
kubectl create secret generic webflux-mongo-secret \
  --from-literal=MONGO_INITDB_ROOT_USERNAME=rootadmin \
  --from-literal=MONGO_INITDB_ROOT_PASSWORD=rootpassword123
```

3) Instalar chart Helm (o chart usa `.Release.Name` para nomear configmap/service):

```powershell
helm upgrade --install webflux ./helm/webflux-chart \
  --set image.repository=webflux-app --set image.tag=latest
```

4) Verifique resources e portas:

```powershell
kubectl get all
kubectl get configmap
kubectl get secret
kubectl port-forward svc/webflux-svc 8091:8091
# ou minikube service
minikube service webflux-svc --url
```

## ☸️ Kubernetes — melhores práticas e pontos a ajustar

- Não construa no pod — use o Dockerfile multi-stage e publique imagens via CI (Docker Hub, GitHub Packages, Harbor, etc.).
- Use `Secrets` para credenciais (Mongo, ES). No Helm chart substitua URIs sensíveis por valores montados a partir de `Secret`.
- Para Mongo em Kubernetes use `StatefulSet` com `PersistentVolumeClaim` (PVC) para dados persistentes. Exemplo:
  - `StatefulSet` com 1 replica + `volumeClaimTemplates` usando storageClass apropriada.
- Para Elasticsearch em Kubernetes prefira usar o chart oficial do Elastic (requer recursos e configuração de security) ou um managed service (Elastic Cloud).
- Configure `readinessProbe` e `livenessProbe` (o chart já inclui probes baseados em Actuator). Também configure `resource.requests` e `resource.limits` para cada container.
- Configure `PodDisruptionBudget` para manter disponibilidade durante upgrades.
- Configure observability: `Prometheus` scrape `/actuator/prometheus`, centralize logs com FluentBit/ELK, traces com Jaeger.

## 🔁 Script de inicialização Elasticsearch (es-init)

O script `docker/es-init/init-es.sh` — já incluído — faz:
- espera ES ficar em estado `yellow` (retry loop);
- cria um `_index_template/webflux_template` para índices `users*` com mappings padrão;
- cria o índice `users` caso não exista.

Se rodar em Kubernetes, converta esse script para um `Job` Helm template ou `initContainer` (preferido: um `Job` que execute uma vez e falhe se não conseguir criar os mappings — Helm pode aguardar o Job completar via hooks ou você pode aplicar o Job manualmente antes do deploy).

## ✅ Checklist para rodar sem erros (resumo rápido)

1) Atualizar `.env` com credenciais corretas.
2) `.\mvnw.cmd -DskipTests package` — gerar JAR e validar build.
3) `docker compose up --build` — para dev local (ver logs e `es-init`).
4) `docker build -t webflux-app:latest .` e `minikube image load webflux-app:latest` para Minikube.
5) `helm upgrade --install webflux ./helm/webflux-chart --set image.repository=webflux-app --set image.tag=latest`.
6) Verificar `kubectl get pods` e `kubectl logs` para diagnosticar problemas.

## 🔎 Troubleshooting rápido

- Erro de autenticação no Mongo: confirme `MONGO_INITDB_ROOT_*` no `.env` e que o `init.js` não sobrescreveu usuários.
- App tenta `localhost:27017` dentro do container: confirme `SPRING_DATA_MONGODB_URI` definida no compose/env/ConfigMap/Secret.
- `es-init` não cria índices: verifique se `elasticsearch` está realmente saudável (`/_cluster/health`) e execute `docker compose run --rm es-init` manualmente.
- Helm: se o pod não inicia, rode `kubectl describe pod <pod>` e `kubectl logs <pod>` (verificar erros de binding de porta, variáveis ausentes, problemas de permissões).

---

Se quiser, eu:
- converto o `es-init` para um `Job` Helm template (roda automaticamente no `helm install`),
- atualizo o Helm chart para usar `Secrets` para credenciais e `PersistentVolumeClaims` para Mongo,
- adiciono `micrometer-registry-prometheus` ao `pom.xml` e configuro scrape no Helm/ServiceMonitor.

Diga qual desses próximos passos prefere que eu implemente que eu sigo com os patches e testes.

---

## 📂 Sobre as pastas `helm/` e `k8s/` — guia didático

Esta seção explica o propósito das pastas `helm/` e `k8s/`, os arquivos que normalmente elas contêm e conceitos Kubernetes relevantes para quem está aprendendo.

Visão geral:
- `k8s/` — manifests YAML "puros" para aplicar diretamente com `kubectl apply -f k8s/` (bom para aprendizado e ambientes pequenos).
- `helm/` — chart Helm que gera manifests parametrizáveis a partir de templates e valores (`values.yaml`). Use Helm para deploys repetíveis e parametrizáveis.

Arquivos comuns e o que ensinam:

- `k8s/deployment.yaml` — define um Deployment (controlador) que garante que N réplicas do container rodem. Conceitos ensinados:
  - Labels & selectors (como o Service encontra as Pods);
  - strategy (rolling updates);
  - containers, args, env, volumeMounts;
  - probes (readiness/liveness) — como o kube usa readiness para controlar rotas e liveness para reiniciar pods com problemas.

- `k8s/service.yaml` — expõe pods internamente no cluster (ClusterIP) ou externamente (NodePort/LoadBalancer). Conceitos:
  - port/targetPort/nodePort;
  - selector para vincular ao Deployment;
  - tipos de serviço e quando usar cada um.

- `k8s/statefulset-mongo.yaml` (sugerido) — para bancos com estado (Mongo) use StatefulSet + PVCs:
  - `volumeClaimTemplates` cria PVCs automáticos por pod;
  - ordenação e identidade estável dos pods (importante para replicaset de bancos);
  - explique uso de StorageClass e provisionamento dinâmico.

- `k8s/pvc.yaml` / `k8s/storageclass.yaml` — descrevem como persistir dados em PVs; útil para entender armazenamento em cloud vs minikube.

- `k8s/secret.yaml` & `k8s/configmap.yaml` — configuração e segredos:
  - `ConfigMap` para dados não sensíveis (URIs, flags);
  - `Secret` para credenciais (base64) e como montar como env ou volume.

- `k8s/hpa.yaml` — Horizontal Pod Autoscaler baseado em métricas (CPU/memory or custom metrics) para escalar réplicas.

- `k8s/ingress.yaml` — Ingress é usado para rotear tráfego HTTP(S) para Services (requer controller como nginx/traefik).

- `k8s/job-es-init.yaml` (opcional) — um Job executa tarefas pontuais (como criar índices no ES). Bom para substituir `es-init` do docker-compose.

Helm chart (`helm/webflux-chart/`) — principais arquivos e como usá-los:

- `Chart.yaml` — metadados do chart (nome, versão, appVersion).
- `values.yaml` — valores padrão (image repository/tag, replicaCount, portas, feature flags). Sempre sobrescreva valores sensíveis no CI ou via `--set`/Secrets.
- `templates/deployment.yaml` — template do Deployment. Os templates usam a linguagem Go templating (`{{ .Values... }}`) para injetar valores.
- `templates/service.yaml` — template do Service para expor a aplicação.
- `templates/configmap.yaml` / `templates/secret.yaml` — geram ConfigMaps/Secrets a partir de `values.yaml` (prefira Secrets para credenciais).
- `templates/job.yaml` — aqui você pode colocar um Job que executa `es-init` (Helm hook ou recurso normal).
- `_helpers.tpl` (opcional) — helpers para gerar nomes consistentes (fullname, name).

Como usar o Helm chart durante desenvolvimento:

1) Ajuste `values.yaml` para apontar para a imagem local ou publique a imagem no registry.
2) Instale o chart com:

```bash
helm upgrade --install webflux ./helm/webflux-chart --set image.repository=webflux-app --set image.tag=latest
```

3) Para debugar, gere os manifests localmente sem aplicar:

```bash
helm template webflux ./helm/webflux-chart --set image.repository=webflux-app --set image.tag=latest > rendered.yaml
kubectl apply -f rendered.yaml # ou inspecione o arquivo
```

Boas práticas ao transformar `docker-compose` em `k8s/` + `helm`:

- Use ConfigMaps para configurações não sensíveis e Secrets para segredos; nunca coloque senhas hardcoded nas templates.
- Evite compilar dentro do pod — crie imagens otimizadas (multi-stage) no CI e referencie a imagem no chart.
- Para bancos (Mongo/Elasticsearch), prefira serviços gerenciados ou rodar fora do cluster; se precisar rodar no cluster, use StatefulSets com PVCs e configure readiness/liveness corretamente.
- Expor Prometheus metrics: adicione `ServiceMonitor` (via Prometheus Operator) ou anote o Service para scraping; adicione a dependência `micrometer-registry-prometheus` no `pom.xml` e exponha `/actuator/prometheus`.
- Para inicializações (es-init), prefira:
  - um `Job` ou Helm hook `pre-install`/`post-install` para executar uma vez, ou
  - um `initContainer` no pod que precisa do recurso (quando apropriado).

Dicas práticas (Minikube)
- Use `minikube image load` para carregar imagens locais no cluster quando `imagePullPolicy: Never`.
- Habilite addons úteis: `minikube addons enable ingress` e `minikube addons enable metrics-server` (necessário para HPA testing).
- Para debug: `kubectl port-forward svc/<service> <local>:<remote>` ou `minikube service <svc> --url`.

Recursos de aprendizagem recomendados
- Kubernetes official docs: https://kubernetes.io/docs/
- Helm docs: https://helm.sh/docs/
- Kubernetes Patterns (book) — padrões de design de aplicações em K8s.

---

Se quiser, eu implemento agora uma das opções sugeridas (Job Helm para `es-init`, Secrets no chart, ou métricas Prometheus). Diga qual prefere e aplico os patches com testes locais e validações de helm template.
 
---

## 🏛️ Produção — Guia prático (obrigatório antes do deploy)

Este bloco reúne exemplos práticos e 'copy/paste' para deixar o chart/manifest prontos para ambientes críticos.

1) Secrets (não usar ConfigMap para credenciais)

Exemplo `values.yaml` (fragmento):
```yaml
mongodb:
  user: "__REPLACE__"
  password: "__REPLACE__"
  db: appdb
```

Exemplo `templates/secret.yaml` (Helm):
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: {{ .Release.Name }}-secret
type: Opaque
stringData:
  mongo_user: "{{ .Values.mongodb.user }}"
  mongo_password: "{{ .Values.mongodb.password }}"
  SPRING_DATA_MONGODB_URI: >-
    mongodb://{{ .Values.mongodb.user }}:{{ .Values.mongodb.password }}@mongo:27018/{{ .Values.mongodb.db }}?authSource=admin
```

Uso no `deployment.yaml` (env via secretKeyRef):
```yaml
env:
  - name: SPRING_DATA_MONGODB_URI
    valueFrom:
      secretKeyRef:
        name: {{ .Release.Name }}-secret
        key: SPRING_DATA_MONGODB_URI
```

Alternativas seguras:
- Use SealedSecrets (bitnami) para armazenar secretos no Git com segurança.
- Ou HashiCorp Vault / External Secrets Operator para rotacionar credenciais.

2) Requests / Limits (essencial)

Adicione em `values.yaml` e injete em `deployment.yaml`:
```yaml
resources:
  requests:
    cpu: "250m"
    memory: "512Mi"
  limits:
    cpu: "1000m"
    memory: "1Gi"
```

Template (snippet):
```yaml
resources:
{{ toYaml .Values.resources | indent 10 }}
```

3) ReplicaCount e PodDisruptionBudget (PDB)

Defina `replicaCount: 3` por padrão em produção e crie um PDB:
```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: {{ .Release.Name }}-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: {{ .Release.Name }}
```

4) Probes: adicionar startupProbe e ajustar thresholds

Exemplo dentro do contêiner:
```yaml
startupProbe:
  httpGet:
    path: /actuator/health
    port: {{ .Values.service.port }}
  periodSeconds: 10
  failureThreshold: 30
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: {{ .Values.service.port }}
  initialDelaySeconds: 40
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: {{ .Values.service.port }}
  initialDelaySeconds: 10
  periodSeconds: 5
```

5) SecurityContext (rodar non-root, reduzir capabilities)

Adicionar ao pod/container:
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 2000
containers:
  - name: app
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop: ["ALL"]
```

6) Anti-affinity / topology spread

Exemplo preferido (não garante, ajuda o scheduler):
```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
              - key: app
                operator: In
                values:
                  - {{ .Release.Name }}
          topologyKey: topology.kubernetes.io/zone
```

7) HPA (Horizontal Pod Autoscaler)

Exemplo HPA v2 usando CPU:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: {{ .Release.Name }}-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: {{ .Release.Name }}-deployment
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 50
```

Nota: habilite `metrics-server` em Minikube ou use Prometheus Adapter para custom metrics.

8) NetworkPolicy (deny-by-default example)

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: {{ .Release.Name }}-netpol
spec:
  podSelector:
    matchLabels:
      app: {{ .Release.Name }}
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: kube-system
        - podSelector:
            matchLabels:
              app: ingress-nginx
      ports:
        - protocol: TCP
          port: {{ .Values.service.port }}
```

9) StatefulSet + PVC para Mongo (exemplo mínimo)

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mongo
spec:
  serviceName: mongo
  replicas: 3
  selector:
    matchLabels:
      app: mongo
  template:
    metadata:
      labels:
        app: mongo
    spec:
      containers:
        - name: mongo
          image: mongo:6.0
          ports:
            - containerPort: 27017
          volumeMounts:
            - name: mongo-data
              mountPath: /data/db
  volumeClaimTemplates:
    - metadata:
        name: mongo-data
      spec:
        accessModes: [ "ReadWriteOnce" ]
        resources:
          requests:
            storage: 10Gi
```

10) Observability — Prometheus & ServiceMonitor

- Adicione `io.micrometer:micrometer-registry-prometheus` ao `pom.xml` para expor `/actuator/prometheus`.
- Template `ServiceMonitor` (Prometheus Operator):
```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: {{ .Release.Name }}-sm
spec:
  selector:
    matchLabels:
      app: {{ .Release.Name }}
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
```

11) Convert `es-init` to Kubernetes Job (Helm)

Exemplo `templates/job-es-init.yaml`:
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ .Release.Name }}-es-init
spec:
  template:
    spec:
      containers:
        - name: es-init
          image: curlimages/curl:8.4.0
          command: ["sh", "-c", "/scripts/init-es.sh"]
          volumeMounts:
            - name: es-init-script
              mountPath: /scripts
      restartPolicy: OnFailure
      volumes:
        - name: es-init-script
          configMap:
            name: {{ .Release.Name }}-es-init-config
```

12) CI/CD — build, tag, push, deploy

Exemplo GitHub Actions snippet (build + push + helm upgrade):
```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
      - name: Build
        run: mvn -B -DskipTests package
      - name: Build Docker image
        run: |
          docker build -t ghcr.io/${{ github.repository }}:${{ github.sha }} .
          docker push ghcr.io/${{ github.repository }}:${{ github.sha }}
      - name: Helm deploy
        run: |
          helm upgrade --install webflux ./helm/webflux-chart --set image.repository=ghcr.io/${{ github.repository }} --set image.tag=${{ github.sha }}
```

13) Image policy & signing

- Use image digests (sha256) or immutable tags produced by CI.
- Consider Cosign/Sigstore to sign images and Gatekeeper policy to enforce signed images.

14) Tracing (opcional, recomendado em fintech)

- Instrument application with OpenTelemetry and export to Jaeger/OTLP. Add OTel SDK and configure exporter via environment variables in k8s.

---

Se quiser, eu aplico automaticamente os patches no chart (Secret + resources + replicas + PDB + securityContext + Job for es-init + change default tag) e gero a renderização simulada dos manifests atualizados para sua revisão. Responda "sim, aplicar patches" ou apenas "não — só README".


