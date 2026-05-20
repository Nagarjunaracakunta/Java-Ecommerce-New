# Observability Guide — ELK Stack (Elasticsearch · Logstash · Kibana)
# Grafana · Dynatrace · Splunk (coming soon)

---

## Table of Contents

### ELK Stack
1. [What Is Already Configured](#1-what-is-already-configured)
2. [How the Log Pipeline Works](#2-how-the-log-pipeline-works)
3. [Start the ELK Stack](#3-start-the-elk-stack)
4. [Verify Elasticsearch and Kibana Are Running](#4-verify-elasticsearch-and-kibana-are-running)
5. [Configure Kibana — First-Time Setup](#5-configure-kibana--first-time-setup)
6. [Create a Data View (Index Pattern)](#6-create-a-data-view-index-pattern)
7. [Check Logs in Discover](#7-check-logs-in-discover)
8. [Write KQL Queries to Filter Logs](#8-write-kql-queries-to-filter-logs)
9. [Build Dashboards in Kibana](#9-build-dashboards-in-kibana)
10. [Add a New Service — Integration Checklist](#10-add-a-new-service--integration-checklist)
11. [Useful Index and API Commands](#11-useful-index-and-api-commands)
12. [Common Errors and Fixes](#12-common-errors-and-fixes)

---

## 1. What Is Already Configured

Everything below is already in the project — you do not need to add anything to run the ELK stack.

### Services that send logs

| Service | `spring.application.name` | Logstash index created |
|---|---|---|
| api-gateway | `api-gateway` | `ecommerce-logs-api-gateway-YYYY.MM.dd` |
| auth-service | `auth-service` | `ecommerce-logs-auth-service-YYYY.MM.dd` |
| product-service | `product-service` | `ecommerce-logs-product-service-YYYY.MM.dd` |
| cart-service | `cart-service` | `ecommerce-logs-cart-service-YYYY.MM.dd` |
| order-service | `order-service` | `ecommerce-logs-order-service-YYYY.MM.dd` |
| payment-service | `payment-service` | `ecommerce-logs-payment-service-YYYY.MM.dd` |
| notification-service | `notification-service` | `ecommerce-logs-notification-service-YYYY.MM.dd` |

### Files that make this work

```
logstash/pipeline/logstash.conf          # Logstash pipeline (TCP → Elasticsearch)
*/src/main/resources/logback-spring.xml  # Each service's logback config
docker-compose.yml                       # elasticsearch, logstash, kibana services
```

### Each service's `logback-spring.xml` does two things

1. Logs to console in human-readable format
2. Sends structured JSON to Logstash on port 5000 (TCP), including a `service_name` field

---

## 2. How the Log Pipeline Works

```
Spring Boot Service
  └─► LogstashTcpSocketAppender (port 5000)
         └─► Logstash (listens on TCP 5000)
                └─► Elasticsearch (index: ecommerce-logs-{service_name}-{date})
                       └─► Kibana (reads from Elasticsearch, port 5601)
```

**Every log line becomes a JSON document in Elasticsearch** with these key fields:

| Field | Example value | Source |
|---|---|---|
| `@timestamp` | `2026-05-20T10:30:00.000Z` | Logstash filter |
| `service_name` | `auth-service` | logback customFields |
| `level` | `INFO` / `ERROR` / `WARN` | logback |
| `message` | `User login successful` | your logger call |
| `logger_name` | `com.example.auth.AuthController` | logback |
| `thread_name` | `http-nio-8080-exec-1` | logback |

docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
---

## 3. Start the ELK Stack

### Option A — Start only the ELK stack (no app services)

```bash
docker compose up -d elasticsearch logstash kibana
```

### Option B — Start everything together

```bash
docker compose up -d
```

### Check container status

```bash
docker compose ps
```

> **Note:** The STATUS column shows `running` on Docker Compose v2 and `Up` on Docker Compose v1 — both mean the container is active.

If you see `Up` instead of `running`, use the native Docker command for a consistent view:

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

To include stopped containers too:

```bash
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Expected output — all three ELK containers should appear with `Up` or `running`:

```
NAMES                      STATUS         PORTS
ecommerce-elasticsearch    Up 2 minutes   0.0.0.0:9200->9200/tcp
ecommerce-logstash         Up 2 minutes
ecommerce-kibana           Up 2 minutes   0.0.0.0:5601->5601/tcp
```

### Watch Logstash start

Logstash takes 30–60 seconds to initialize:

```bash
docker compose logs -f logstash
```

Wait until you see:
```
[INFO ] Pipelines running {:count=>1, :running_pipelines=>[:main], :non_running_pipelines=>[]}
```

---

## 4. Verify Elasticsearch and Kibana Are Running

### Elasticsearch health check

```bash
curl http://localhost:9200/_cluster/health?pretty
```

Expected: `"status" : "green"` or `"status" : "yellow"` (yellow is normal for single-node)

### Elasticsearch node info

```bash
curl http://localhost:9200
```

### Kibana health check

```bash
curl http://localhost:5601/api/status
```

Or just open **http://localhost:5601** in a browser.

### Check that indices exist (after services send logs)

```bash
curl http://localhost:9200/_cat/indices?v
```

You should see rows like:
```
yellow  ecommerce-logs-auth-service-2026.05.20
yellow  ecommerce-logs-order-service-2026.05.20
```

---

## 5. Configure Kibana — First-Time Setup

1. Open **http://localhost:5601** in a browser.
2. If prompted with "Welcome to Kibana" or "Select your space", click **Explore on my own**.
3. If asked for credentials — this setup has `xpack.security.enabled=false`, so there is no login required.
4. You are now on the Kibana home screen.
   http://localhost:5601/app/dashboards#/view/dashboard-microservices?_g=(filters:!(),refreshInterval:(pause:!f,value:10000),time:(from:now-24h,to:now))
**Navigation shortcut**: Click the hamburger menu (☰) at the top-left to open the full menu.

---

## 6. Create a Data View (Index Pattern)

A Data View tells Kibana which Elasticsearch indices to read. You only do this once per pattern.

**Step-by-step:**

1. Open menu → **Stack Management** (under Management section at the bottom).
2. Click **Data Views** (under Kibana section).
3. Click **Create data view**.
4. Fill in:
   - **Name**: `ecommerce-logs`
   - **Index pattern**: `ecommerce-logs-*`  ← the `*` matches all services and all dates
   - **Timestamp field**: `@timestamp`
5. Click **Save data view to Kibana**.

You now have one data view that covers logs from all 7 services across all dates.

**Optional — per-service data views:**

Repeat the steps above with a narrower pattern if you want to isolate one service:

| Name | Index pattern |
|---|---|
| auth-service logs | `ecommerce-logs-auth-service-*` |
| order-service logs | `ecommerce-logs-order-service-*` |
| payment-service logs | `ecommerce-logs-payment-service-*` |

---

## 7. Check Logs in Discover

**Discover** is Kibana's raw log viewer — like a searchable tail of your logs.

1. Open menu → **Discover** (under Analytics).
2. In the top-left dropdown, select **ecommerce-logs** (the data view you created).
3. Set the time range in the top-right — start with **Last 15 minutes** or **Last 1 hour**.
4. Each row is one log entry. Click any row to expand it and see all fields.

### Customize the columns

By default Discover shows `@timestamp` and the raw `_source` document. Add useful columns:

1. In the left panel under **Available fields**, hover over a field.
2. Click **+** to add it as a column.

Recommended columns to add:
- `service_name`
- `level`
- `message`
- `logger_name`

---

## 8. Write KQL Queries to Filter Logs

KQL (Kibana Query Language) is typed into the search bar at the top of Discover.

### Basic filters

```kql
# All logs from one service
service_name : "auth-service"

# All ERROR logs
level : "ERROR"

# Errors from one service
service_name : "order-service" AND level : "ERROR"

# Search message text (contains)
message : "login"

# Exact phrase in message
message : "User not found"

# Logs NOT from a service
NOT service_name : "notification-service"
```

### Useful queries for this project

```kql
# All payment failures
service_name : "payment-service" AND level : "ERROR"

# JWT / auth issues
service_name : "auth-service" AND message : "JWT"

# Any exception across all services
message : "Exception"

# Kafka consumer errors in notification service
service_name : "notification-service" AND level : "ERROR"

# Cart service warnings
service_name : "cart-service" AND level : "WARN"

# All non-INFO logs (WARN + ERROR)
level : "WARN" OR level : "ERROR"

# Logs from a specific class
logger_name : "com.example.order.service.OrderService"
```

### Save a search

1. Run a KQL query you use often.
2. Click **Save** (top-right) → give it a name like `Payment Errors`.
3. Saved searches can be added directly to dashboards.

---

## 9. Build Dashboards in Kibana

Dashboards let you combine multiple visualizations (charts, tables, counters) into one screen.

### Step 1 — Create visualizations

Go to menu → **Visualize Library** → **Create visualization**.

Choose **Lens** (the recommended editor — drag and drop).

---

#### Visualization 1 — Log count by service (Bar chart)

This shows how many logs each service produces over time.

1. Create visualization → **Lens**.
2. Select data view: `ecommerce-logs`.
3. Chart type: **Bar vertical stacked** (top-left dropdown).
4. Drag **`@timestamp`** to the **X-axis**.
5. Drag **`service_name`** to **Break down by**.
6. Y-axis defaults to **Count of records** — leave it.
7. Click **Save and return** (or **Save to library**).
8. Name it: `Log Volume by Service`.

---

#### Visualization 2 — Error rate by service (Bar chart)

1. Create visualization → **Lens**.
2. Add a KQL filter at the top: `level : "ERROR"`.
3. Drag **`service_name`** to the X-axis.
4. Y-axis: **Count of records**.
5. Chart type: **Bar vertical**.
6. Save as: `Error Count by Service`.

---

#### Visualization 3 — Log level breakdown (Pie chart)

1. Create visualization → **Lens**.
2. Chart type: **Pie**.
3. Drag **`level`** to the **Slice by** field.
4. Metric: **Count of records**.
5. Save as: `Log Level Distribution`.

---

#### Visualization 4 — Errors over time (Line chart)

1. Create visualization → **Lens**.
2. Add filter: `level : "ERROR"`.
3. Chart type: **Line**.
4. Drag **`@timestamp`** to the X-axis (set interval to `Auto`).
5. Y-axis: **Count of records**.
6. Drag **`service_name`** to **Break down by**.
7. Save as: `Error Trend Over Time`.

---

#### Visualization 5 — Recent errors table

1. Create visualization → **Lens**.
2. Add filter: `level : "ERROR"`.
3. Chart type: **Table**.
4. Rows: drag **`@timestamp`**, **`service_name`**, **`message`**, **`logger_name`**.
5. Metric: **Count**.
6. Save as: `Recent Error Messages`.

---

### Step 2 — Create the Dashboard

1. Go to menu → **Dashboard** → **Create dashboard**.
2. Click **Add from library**.
3. Select the visualizations you saved: `Log Volume by Service`, `Error Count by Service`, `Log Level Distribution`, `Error Trend Over Time`, `Recent Error Messages`.
4. Drag and resize panels to your preferred layout.
5. Click **Save** → Name it: `Ecommerce Services Overview`.

### Step 3 — Add a time filter control (optional)

1. Inside the dashboard, click **Controls** → **Add control**.
2. Add a **Time slider** so viewers can shift the time window without leaving the dashboard.

### Step 4 — Set a refresh interval

1. In the dashboard, click the time picker (top-right).
2. Click **Refresh every** → set to `10 seconds` or `1 minute` for live monitoring.

---

### Suggested dashboard layout

```
┌─────────────────────────────┬─────────────────────┐
│  Log Volume by Service      │  Log Level          │
│  (Bar stacked, full width)  │  Distribution (Pie) │
├─────────────────────────────┴─────────────────────┤
│  Error Trend Over Time (Line chart, full width)   │
├───────────────────┬───────────────────────────────┤
│  Error Count by   │  Recent Error Messages        │
│  Service (Bar)    │  (Table)                      │
└───────────────────┴───────────────────────────────┘
```

---

## 10. Add a New Service — Integration Checklist

When you create a new microservice, follow this checklist to send its logs to Kibana automatically.

### Step 1 — Add the Logstash dependency to `pom.xml`

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### Step 2 — Create `logback-spring.xml` in `src/main/resources/`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name" defaultValue="unknown-service"/>
    <springProperty scope="context" name="logstashHost" source="LOGSTASH_HOST" defaultValue="localhost"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>${logstashHost}:5000</destination>
        <reconnectionDelay>10 seconds</reconnectionDelay>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service_name":"${appName}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>
```

### Step 3 — Set `spring.application.name` in `application.properties`

```properties
spring.application.name=your-service-name
```

### Step 4 — Add `LOGSTASH_HOST` to `docker-compose.yml`

```yaml
your-service:
  environment:
    - LOGSTASH_HOST=logstash
```

### Step 5 — Kibana — update the data view

The existing `ecommerce-logs-*` data view already covers the new service automatically because of the `*` wildcard. No Kibana change needed.

To verify: after starting the service, check for its index:
```bash
curl "http://localhost:9200/_cat/indices/ecommerce-logs-your-service-name*?v"
```

---

## 11. Useful Index and API Commands

### List all ecommerce log indices

```bash
curl "http://localhost:9200/_cat/indices/ecommerce-logs-*?v&s=index"
```

### Count documents in an index

```bash
curl "http://localhost:9200/ecommerce-logs-auth-service-$(date +%Y.%m.%d)/_count"
```

### Search logs via REST API (without Kibana)

```bash
# All errors from order-service today
curl -X GET "http://localhost:9200/ecommerce-logs-order-service-$(date +%Y.%m.%d)/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "term": { "level": "ERROR" }
    },
    "sort": [{ "@timestamp": "desc" }],
    "size": 20
  }'
```

```bash
# Search message text across all services
curl -X GET "http://localhost:9200/ecommerce-logs-*/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "match": { "message": "payment failed" }
    },
    "sort": [{ "@timestamp": "desc" }],
    "size": 10
  }'
```

### Delete old indices (free up disk)

```bash
# Delete a specific day's index
curl -X DELETE "http://localhost:9200/ecommerce-logs-auth-service-2026.05.01"

# Delete all indices older than a pattern
curl -X DELETE "http://localhost:9200/ecommerce-logs-*-2026.04.*"
```

### Check Logstash pipeline is receiving events

```bash
docker exec ecommerce-logstash curl -s "http://localhost:9600/_node/stats/pipelines?pretty" \
  | grep -E "events_in|events_out"
```

### Tail logs from a running container

```bash
# Real-time Logstash logs
docker compose logs -f logstash

# Real-time Elasticsearch logs
docker compose logs -f elasticsearch

# Real-time from all three ELK containers
docker compose logs -f elasticsearch logstash kibana
```

---

## 12. Common Errors and Fixes

### Kibana shows "No results found" in Discover

**Cause 1 — Wrong time range**
- Check the time picker (top-right). Set it to `Last 1 hour` or `Last 24 hours`.

**Cause 2 — Data view not matching any indices**
- Go to Stack Management → Data Views → verify the pattern is `ecommerce-logs-*`.
- Run `curl http://localhost:9200/_cat/indices?v` to confirm indices exist.

**Cause 3 — Services have not sent any logs yet**
- Start the Spring Boot services: `docker compose up -d auth-service product-service`.
- Make an HTTP request to any service to generate log output.
- Wait 30 seconds, then refresh Discover.

---

### Logstash fails to start

**Symptom**: `docker compose ps` shows logstash as `exited`

**Fix**: Check logs:
```bash
docker compose logs logstash | tail -30
```

Common causes:
- Elasticsearch not ready yet — wait 30 seconds and run `docker compose restart logstash`
- Config syntax error in `logstash/pipeline/logstash.conf`

---

### Services show `Connection refused` to Logstash

**Symptom**: Service logs show `Failed to connect to logstash:5000`

**Cause**: Service started before Logstash was ready.

**Fix**:
```bash
docker compose restart auth-service product-service cart-service order-service payment-service notification-service api-gateway
```

The `reconnectionDelay: 10 seconds` in `logback-spring.xml` means services will auto-reconnect after 10 seconds — a restart is usually not needed, just wait.

---

### Elasticsearch shows `yellow` status

This is **normal for single-node** deployments. Elasticsearch marks replica shards as unassigned (yellow) because there is only one node. All data is stored and searchable — yellow just means no replicas exist.

To suppress the warning, set `number_of_replicas: 0` as the default:

```bash
curl -X PUT "http://localhost:9200/_settings" \
  -H "Content-Type: application/json" \
  -d '{"index": {"number_of_replicas": 0}}'
```

---

### "Field X is not indexed" error in Kibana visualizations

**Fix**: Go to Stack Management → Data Views → click `ecommerce-logs` → click the **Refresh** icon to re-sync field mappings after new logs arrive.

---

### Kibana is slow or crashes

The `kibana` service has `mem_limit: 512m` in `docker-compose.yml`. On machines with less than 8 GB RAM, increase it:

```yaml
kibana:
  mem_limit: 768m
  environment:
    - NODE_OPTIONS=--max-old-space-size=512
```

---

## Coming Soon

- **Grafana** — metrics dashboards with Prometheus data source
- **Dynatrace** — APM, distributed tracing, infrastructure monitoring
- **Splunk** — enterprise log aggregation and SIEM integration

---

*Guide version: May 2026 — covers Elasticsearch 8.11.0, Logstash 8.11.0, Kibana 8.11.0*