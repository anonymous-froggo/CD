.extern putchar
.extern get_stdout
.extern fflush
.global main
.global _main
.text
main:
  call _main
  movq %rax, %rdi
  movq $0x3C, %rax
  pushq %rax
  pushq %rcx
  pushq %rdx
  pushq %rsi
  pushq %rdi
  pushq %r8
  pushq %r9
  pushq %r10
  pushq %r11
  call _flush
  popq %r11
  popq %r10
  popq %r9
  popq %r8
  popq %rdi
  popq %rsi
  popq %rdx
  popq %rcx
  popq %rax
  syscall
