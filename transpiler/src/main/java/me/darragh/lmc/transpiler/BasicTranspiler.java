package me.darragh.lmc.transpiler;

import lombok.RequiredArgsConstructor;
import me.darragh.lmc.Instruction;
import me.darragh.lmc.Opcode;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * A basic transpiler for LMC. It generates a Java class with static
 * fields for memory and the accumulator, and a main method that implements the LMC logic.
 * <br/>
 * <h1>Limitation</h1>
 * The maximum size of a value is the integer limit, not following the 3-digit limit of LMC.
 * This allows for more complex programs, but may not be faithful to the original specification.
 *
 * @author darraghd493
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class BasicTranspiler implements Transpiler, Opcodes {
    public static final String CLASS_NAME = "LMCProgram";
    private static final String MEM_FIELD_NAME = "memory";
    private static final String ACC_FIELD_NAME = "accumulator";
    private static final String SCAN_FIELD_NAME = "scanner";

    private final Map<Integer, Integer> memoryIndexTranslations = new HashMap<>();

    private final @NotNull Instruction[] instructions;
    private ClassNode classNode;

    @Override
    public void build() {
        ClassNode classNode = new ClassNode();
        classNode.name = CLASS_NAME;
        classNode.access = ACC_PUBLIC;
        classNode.version = V1_8;
        classNode.superName = "java/lang/Object";

        // Step 1: fields
        classNode.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC, ACC_FIELD_NAME, "I", null, 0));
        classNode.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC, MEM_FIELD_NAME, "[I", null, null));
        classNode.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC, SCAN_FIELD_NAME, "Ljava/util/Scanner;", null, null));

        // Step 2: static init (<clinit>)
        MethodNode clinit = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
        InsnList ci = clinit.instructions;

        // init scanner - used for input
        // Scanner scanner = new Scanner(System.in);
        ci.add(new TypeInsnNode(NEW, "java/util/Scanner"));
        ci.add(new InsnNode(DUP));
        ci.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;"));
        ci.add(new MethodInsnNode(INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false));
        ci.add(new FieldInsnNode(PUTSTATIC, CLASS_NAME, SCAN_FIELD_NAME, "Ljava/util/Scanner;"));

        // pre-count DAT instructions to determine memory size
        int datCount = 0;
        for (Instruction instruction : instructions) {
            if (instruction.opcode() == Opcode.DAT) datCount++;
        }

        // init memory array
        // memory = new int[instructions.length]
        ci.add(pushInt(datCount));
        ci.add(new IntInsnNode(NEWARRAY, T_INT));

        // fill DAT values into array
        int instructionDats = 0;
        for (int i = 0; i < instructions.length; i++) {
            Instruction instruction = instructions[i];
            if (instruction.opcode() == Opcode.DAT) {
                this.memoryIndexTranslations.put(i, instructionDats);
                ci.add(new InsnNode(DUP)); // duplicate array ref
                ci.add(pushInt(instructionDats++)); // index
                ci.add(pushInt(instruction.operand())); // value
                ci.add(new InsnNode(IASTORE));
            }
        }
        ci.add(new FieldInsnNode(PUTSTATIC, CLASS_NAME, MEM_FIELD_NAME, "[I"));
        ci.add(new InsnNode(RETURN));
        classNode.methods.add(clinit);

        // Step 3: LMC program logic (main method)
        MethodNode main = new MethodNode(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        Map<Integer, LabelNode> labels = new HashMap<>();
        for (int i = 0; i < this.instructions.length; i++) {
            labels.put(i, new LabelNode(new Label()));
        }

        for (int i = 0; i < this.instructions.length; i++) {
            Instruction ins = this.instructions[i];
            LabelNode l = labels.get(i);
            main.instructions.add(new LineNumberNode(i, l));
            main.instructions.add(l);

            int operandLocation = ins.operand() == null ? -1 : this.getMemoryLocation(ins.operand());

            switch (ins.opcode()) {
                case HLT -> main.instructions.add(new InsnNode(RETURN));
                case ADD -> {
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, MEM_FIELD_NAME, "[I"));
                    main.instructions.add(pushInt(operandLocation));
                    main.instructions.add(new InsnNode(IALOAD));
                    main.instructions.add(new InsnNode(IADD));
                    main.instructions.add(new FieldInsnNode(PUTSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                }
                case SUB -> {
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, MEM_FIELD_NAME, "[I"));
                    main.instructions.add(pushInt(operandLocation));
                    main.instructions.add(new InsnNode(IALOAD));
                    main.instructions.add(new InsnNode(ISUB));
                    main.instructions.add(new FieldInsnNode(PUTSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                }
                case STA, STO -> {
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, MEM_FIELD_NAME, "[I"));
                    main.instructions.add(pushInt(operandLocation));
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                    main.instructions.add(new InsnNode(IASTORE));
                }
                case LDA -> {
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, MEM_FIELD_NAME, "[I"));
                    main.instructions.add(pushInt(operandLocation));
                    main.instructions.add(new InsnNode(IALOAD));
                    main.instructions.add(new FieldInsnNode(PUTSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                }
                case BRA -> main.instructions.add(new JumpInsnNode(GOTO, labels.get(operandLocation)));
                case BRZ -> {
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                    main.instructions.add(new JumpInsnNode(IFEQ, labels.get(operandLocation)));
                }
                case BRP -> {
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                    main.instructions.add(new JumpInsnNode(IFGE, labels.get(operandLocation)));
                }
                case INP -> {
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, SCAN_FIELD_NAME, "Ljava/util/Scanner;"));
                    main.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Scanner", "nextInt", "()I", false));
                    main.instructions.add(new FieldInsnNode(PUTSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                }
                case OUT -> {
                    main.instructions.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                    main.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false));
                }
                case OTC -> {
                    main.instructions.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                    main.instructions.add(new FieldInsnNode(GETSTATIC, CLASS_NAME, ACC_FIELD_NAME, "I"));
                    main.instructions.add(new InsnNode(I2C));
                    main.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "print", "(C)V", false));
                }
            }
        }

        main.instructions.add(new InsnNode(RETURN));
        classNode.methods.add(main);
        this.classNode = classNode;
    }

    @Override
    public void save(Path path) throws IOException {
        if (Files.exists(path)) Files.delete(path);
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(path))) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            this.classNode.accept(cw);
            jos.putNextEntry(new JarEntry(CLASS_NAME + ".class"));
            jos.write(cw.toByteArray());
            jos.closeEntry();

            jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            String manifest = "Manifest-Version: 1.0\nMain-Class: %s\n".formatted(CLASS_NAME);
            jos.write(manifest.getBytes());
            jos.closeEntry();
        }
    }

    private int getMemoryLocation(int instructionLocation) {
        int memoryLocation = this.memoryIndexTranslations.size();
        this.memoryIndexTranslations.put(instructionLocation, memoryLocation);
        return memoryLocation;
    }

    private static AbstractInsnNode pushInt(int value) {
        if (value >= -1 && value <= 5) return new InsnNode(ICONST_0 + value);
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) return new IntInsnNode(BIPUSH, value);
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) return new IntInsnNode(SIPUSH, value);
        return new LdcInsnNode(value);
    }
}
