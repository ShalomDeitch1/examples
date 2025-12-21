FROM postgres:16

COPY start-replica.sh /usr/local/bin/start-replica.sh
RUN chmod +x /usr/local/bin/start-replica.sh

ENTRYPOINT ["/usr/local/bin/start-replica.sh"]
CMD ["postgres"]
