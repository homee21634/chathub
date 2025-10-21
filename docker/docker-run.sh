docker run --name postgres-chat \
  -e POSTGRES_PASSWORD=your_password \
  -e POSTGRES_DB=chatdb \
  -p 5433:5432 \
  -d postgres:15

docker exec -it postgres-chat psql -U postgres -d chatdb

docker-compose up -d redis

docker exec -it chat-redis redis-cli ping