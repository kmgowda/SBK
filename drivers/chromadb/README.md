# ChromaDB Driver for SBK Framework

This driver provides performance benchmarking capabilities for ChromaDB, a vector database for AI applications.

## Overview

ChromaDB is an open-source vector database designed for AI applications. This driver allows you to benchmark ChromaDB's performance for storing and retrieving byte arrays as documents within collections.

## Features

- **Vector Storage**: Stores data as documents in ChromaDB collections
- **Automatic Embeddings**: Uses default embedding function for vector generation
- **Configurable Connection**: Supports custom host, port, and SSL settings
- **Collection Management**: Automatically creates collections if they don't exist
- **Metadata Tracking**: Stores metadata including writer ID, data size, and timestamps

## Configuration

The driver supports the following configuration options:

- `host`: ChromaDB server host (default: localhost)
- `port`: ChromaDB server port (default: 8000)
- `collectionName`: Name of the collection to use (default: sbk_benchmark)
- `embeddingDimension`: Dimension for embeddings (default: 384)
- `distanceFunction`: Distance function for similarity (default: cosine)
- `ssl`: Use SSL connection (default: false)
- `authToken`: Authentication token (default: empty)
- `timeoutSeconds`: Connection timeout in seconds (default: 30)
- `maxRetries`: Maximum connection retries (default: 3)
- `batchSize`: Batch size for operations (default: 100)

## Prerequisites

1. **ChromaDB Server**: You need a running ChromaDB instance
   ```bash
   # Using Docker
   docker run -p 8000:8000 chromadb/chroma
   
   # Or using Python
   pip install chromadb
   chroma run --host localhost --port 8000
   ```

2. **Java Dependencies**: The driver automatically includes the required ChromaDB Java client

## Usage

### Basic Usage

```bash
# Run benchmark with default settings
./sbk -class chromadb -writers 4 -readers 4 -size 1024 -seconds 60

# Custom ChromaDB settings
./sbk -class chromadb -writers 4 -readers 4 -size 1024 -seconds 60 \
  -host localhost \
  -port 8000 \
  -collectionName my_test_collection
```

### Configuration File

You can also modify the default settings in `ChromaDB.properties`:

```properties
host=localhost
port=8000
collectionName=sbk_benchmark
embeddingDimension=384
distanceFunction=cosine
ssl=false
authToken=
timeoutSeconds=30
maxRetries=3
batchSize=100
```

## Implementation Details

### Data Storage

- **Write Operations**: Data is stored as documents in ChromaDB collections
- **Read Operations**: Documents are retrieved by their unique IDs
- **Embeddings**: Automatically generated using the default embedding function
- **Metadata**: Each document includes writer ID, data size, and timestamp

### Key Generation

The driver uses a key generation strategy similar to other SBK drivers:
- Each writer/reader gets a unique starting key based on their ID
- Keys increment sequentially for each operation

### Error Handling

- Connection failures are handled with configurable retries
- EOF exceptions are properly handled for read operations
- All exceptions are wrapped in IOException for consistency

## Performance Considerations

- **Batch Processing**: The driver supports batch operations for improved throughput
- **Connection Reuse**: Connections are reused across operations
- **Embedding Overhead**: Embedding generation adds computational overhead
- **Network Latency**: Performance depends on network connectivity to ChromaDB

## Troubleshooting

### Common Issues

1. **Connection Refused**: Ensure ChromaDB server is running and accessible
2. **Collection Not Found**: The driver automatically creates collections
3. **Memory Issues**: Monitor heap size for large datasets
4. **Slow Performance**: Consider batch sizes and network optimization


## Dependencies

- `io.github.amikos-tech:chromadb-java-client:0.1.7` - ChromaDB Java client
- `tools.jackson` - JSON processing (included via ChromaDB client)

## License

This driver is licensed under the Apache License 2.0, same as the SBK framework.
