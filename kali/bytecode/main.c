#include "common.h"
#include "chunk.h"
#include "debug.h"

int main(int argc, const char* argv[]){
  Chunk c;
  initChunk(&c);
  writeChunk(&c, OP_RETURN);

  disassembleChunk(&c, "test chunk");

  freeChunk(&c);

  return 0;
}