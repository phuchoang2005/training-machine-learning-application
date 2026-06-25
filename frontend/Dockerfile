# Serves the locally-built `dist/` — run `npm run build` on the host first.
FROM nginx:1.27-alpine
ENV API_UPSTREAM=http://host.docker.internal:8080
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY dist /usr/share/nginx/html
EXPOSE 80
