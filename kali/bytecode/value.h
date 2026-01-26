#ifndef cali_value_h
#define cali_value_h

#include "common.h"
#include "chunk.h"
#include "value.h"

typedef double Value; //int
typedef struct { //value array
  int capacity;
  int count;
  Value* values;
} ValueArray;

void initValueArray(ValueArray* array);
void writeValueArray(ValueArray* array, Value value);
int addConstant(Chunk* chunk, Value value);
void freeValueArray(ValueArray* array);
void printValue(Value value);




#endif