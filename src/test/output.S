.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq %rax, %rdi
# move the exit syscall number into rax
movq $0x3C, %rax
syscall
_main:
  movq $0, %r8
  movq $10, %r9
  movq %r8, %r9
  subq %r9, %r10
  movq $4, %r11
  movq %r8, %r11
  subq %r11, %r12
  movq %r10, %r12
  imulq %r12, %r13
  movq %r13, %rax
  ret
