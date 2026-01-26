#ifndef cali_chunk_h
#define cali_chunk_h

#include "value.h"
#include "common.h"

typedef enum {
  OP_RETURN,
  OP_CONSTANT,
} OpCode;

typedef struct {
  int count;
  int capacity;
  uint8_t* codes;
  ValueArray constants;
  int* lines;
} Chunk;

void initChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, uint8_t byte, int line);
void freeChunk(Chunk* chunk);
int addConstant(Chunk* chunk, Value value);

#endif