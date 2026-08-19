# Stage 1: Build the Angular application
FROM node:20 AS build

WORKDIR /usr/src/app

# Copy package files first for caching npm install
COPY package.json package-lock.json* ./

# Install dependencies with legacy-peer-deps flag
RUN npm install --legacy-peer-deps

# Copy the rest of the source code
COPY . .

# Build the Angular app
#RUN npm run build
RUN npx ng build template --configuration production


# Stage 2: Serve the app with Nginx
FROM nginx:1.23-alpine

# Copy SSL/TLS certificate files if needed (optional)
#COPY domain.crt /etc/certi/domain.crt
#COPY domain.key /etc/certi/domain.key

# Copy built Angular app files to Nginx html directory
COPY --from=build /usr/src/app/dist/template/browser /usr/share/nginx/html

#  this line to use your custom Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose ports for HTTP and HTTPS
EXPOSE 80
EXPOSE 443

CMD ["nginx", "-g", "daemon off;"]

#COPY entrypoint.sh /entrypoint.sh
#RUN chmod +x /entrypoint.sh

#ENTRYPOINT ["/entrypoint.sh"]

# Copy your nginx configs (adjust filenames if needed)
#COPY nginx-http.conf /etc/nginx/conf.d/default.conf
#COPY nginx-https.conf /etc/nginx/conf.d/https.conf

# Start Nginx and generate env.js from template
# CMD ["/bin/sh", "-c", "envsubst < /usr/share/nginx/html/assets/env.template.js > /usr/share/nginx/html/assets/env.js && exec nginx -g 'daemon off;'"]
