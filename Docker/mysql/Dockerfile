FROM mysql:latest

USER mysql

ENV MYSQL_USER=nikhil
ENV MYSQL_PASSWORD=nikhil123
ENV MYSQL_DATABASE=trajview
ENV MYSQL_ROOT_PASSWORD=root

COPY grantprivileges.sql /docker-entrypoint-initdb.d/

# Expose port 3306 to allow connections to the database
#EXPOSE 3306

# Start the MySQL server when the container is run
CMD ["mysqld"]