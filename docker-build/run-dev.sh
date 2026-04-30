docker run -v download:/app/download -v sample:/app/sample -v web:/app/web -p 8090:8090 --name lipidserver-container lipidxte2/lipid-server

docker compose up --watch
