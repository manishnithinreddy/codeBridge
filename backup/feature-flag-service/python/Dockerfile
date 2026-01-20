FROM python:3.9-slim

WORKDIR /app

# Copy requirements first for better caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application
COPY . .

# Install the package
RUN pip install -e .

# Expose ports
EXPOSE 8090 9090

# Run the application
CMD ["python", "-m", "feature_flag_service.main"]

