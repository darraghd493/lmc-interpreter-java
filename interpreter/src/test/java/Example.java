import me.darragh.lmc.Instruction;
import me.darragh.lmc.interpreter.*;

import java.util.Scanner;

/**
 * @author darraghd493
 * @since 1.0.0
 */
public class Example {
    private static final String SOURCE = """
            // program:
            // > "hello"
            // > brp (didn't manage to include)
            // > take in input for question q{n} 1-3, add q num to value
            // > request each question until complete
            // > "bye"
            
            
            // "hello"
            	lda char_h
            	otc
            	lda char_e
            	otc
            	lda char_l
            	otc
            	otc
            	lda char_o
            	otc
            
            // brp (didn't include)
            	lda two
            	sub one
            	brp start
            	hlt
            
            // input + 1
            start   inp
                    add one
                    out
                    sta ans_1
            
            // input + 2
                    inp
                    add two
                    out
                    sto ans_2
            
            // input + 3
                    inp
                    add three
                    out
                    sto ans_3
            
            // question 1
            q1      lda char_q
                    otc
                    lda char_1
                    otc
            
                    inp
            	sub ans_1
            	brz q2
            	bra q1
            
            q2  lda char_q
            	otc
            	lda char_2
            	otc
            
            	inp
            	sub ans_2
            	brz q3
            	bra q2
            
            q3	lda char_q
            	otc
            	lda char_3
            	otc
            
            	inp
            	sub ans_3
            	brz bye
            	bra q3
            
            bye	lda char_b
            	otc
            	lda char_y
            	otc
            	lda char_e
            	otc
            	hlt
            
            
            
            one     dat 1
            two     dat 2
            three   dat 3
            ans_1   dat 0
            ans_2   dat 0
            ans_3   dat 0
            
            // used for hello
            char_h  dat 104
            char_e  dat 101
            char_l  dat 108
            char_o  dat 111
            
            // used for bye
            char_b  dat 98
            char_y  dat 121\s
            
            // used for q{n}
            char_q  dat 113
            char_1  dat 49
            char_2  dat 50
            char_3  dat 51
            """;

    public static void main(String[] args) throws InterpreterException {
        System.out.println("Parsing...");
        Parser parser = Parser.builder().build();
        Instruction[] instructions = parser.parse(SOURCE);
        for (Instruction instruction : instructions) {
            System.out.printf("%s\t%s%n", instruction.opcode().getOpcodeString(), instruction.operand());
        }
        FaithfulInterpreter interpreter = FaithfulInterpreter.builder()
                .instructions(instructions)
                .ioHandler(IoHandler.of(
                        () -> {
                            Scanner scanner = new Scanner(System.in);
                            System.out.print("Input: ");
                            return scanner.nextInt();
                        },
                        (value, character) -> {
                            if (character) {
                                System.out.print((char) value);
                            } else {
                                System.out.println(value);
                            }
                        }
                )).build();
        interpreter.run();
    }
}
