.text
.global main
.global _main
.extern getchar
.extern putchar
.extern fflush
.extern stdout
main:
  call _main
  push %rax
  mov stdout(%rip), %rdi
  call fflush
  pop %rax
  movq %rax, %rdi
  movq $0x3C, %rax
  syscall
