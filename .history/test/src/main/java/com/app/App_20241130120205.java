package com.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.jocl.CL;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_ONLY;
import static org.jocl.CL.CL_MEM_WRITE_ONLY;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueueWithProperties;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseKernel;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;


public class App 
{

    private static final String KERNEL_CODE =
    "__kernel void count_word(" +
    "    __global const char *text," +
    "    __global const char *word," +
    "    const int text_length," +
    "    const int word_length," +
    "    __global int *count) {" +
    "    const char case_diff = 'a' - 'A';" +
    "    int id = get_global_id(0);" +
    "    if (id + word_length > text_length) {" +
    "        return;" +
    "    }" +
    "    int match = 1;" +
    "    for (int i = 0; i < word_length; i++) {" +
    "        char text_char = text[id + i];" +
    "        char word_char = word[i];" +
    "        if (text_char >= 'A' && text_char <= 'Z') {" +
    "            text_char += case_diff;" +
    "        }" +
    "        if (word_char >= 'A' && word_char <= 'Z') {" +
    "            word_char += case_diff;" +
    "        }" +
    "        if (text_char != word_char) {" +
    "            match = 0;" +
    "            break;" +
    "        }" +
    "    }" +
    "    if (match) {" +
    "        int is_start_valid = (id == 0 || text[id - 1] == ' ' || text[id - 1] == '\\n' || text[id - 1] == '.' || text[id - 1] == ',');" +
    "        int is_end_valid = (id + word_length == text_length || text[id + word_length] == ' ' || text[id + word_length] == '\\n' || text[id + word_length] == '.' || text[id + word_length] == ',');" +
    "        if (is_start_valid && is_end_valid) {" +
    "            atomic_add(count, 1);" +
    "        }" +
    "    }" +
    "}";
    public static void main( String[] args )
    {
        // System.out.println( "Hello World!" );
        String texto = readFile("test/src/main/java/com/files/carlosText.txt");
        // for (String word : arr) {
            //     System.out.println(word);
            
            // }
        String[] arr = stringToArray(texto);
        long inicio = System.nanoTime();
        int qtd = contadorPalavra(arr, "carlos");
        long fim = System.nanoTime();

        System.out.println(qtd);
        System.out.println((fim-inicio)/1000000  + "Milisegundos");
        // System.out.println(texto);
        long inicio_gpu = System.nanoTime();

        startingOpenCL(texto, "Carlos");
        long fim_gpu = System.nanoTime();
        System.out.println(fim_gpu -inicio_gpu/1000000  + "Milisegundos");
        
    }

    static  String readFile(String arquivo){
        StringBuilder conteudo = new StringBuilder();
        try {
            File myArq = new File(arquivo);
            Scanner myReader = new Scanner(myArq);
            while(myReader.hasNextLine()){
                String data = myReader.nextLine();
                // System.out.println(data);
                conteudo.append((data)).append("\n");
            }
            // System.out.println(finalData);
            myReader.close();            
            // return conteudo.toString();
        } catch (FileNotFoundException e) {
            System.out.println("Erro boy");
            e.printStackTrace();
        }
        return  conteudo.toString();
    }
    static  String[] stringToArray(String texto){
        String[] palavras = texto.split("[\\s,\\.\\n\\r]+");
        return  palavras;
    }

    static  int contadorPalavra(String [] texto, String keyword){
        int counter = 0;
        if (texto.length > 0){
            for (String word : texto) {
                if(word.equalsIgnoreCase(keyword)){
                    counter++;
                }
            }
        }else{
            System.out.println("Texto Vazio");
        }
        if(counter == 0){
            System.out.println("A palavra não foi encontrada!");
        }
        return counter;

    }

    static byte[] transformarBytes(String texto){
        String[] arr = stringToArray(texto);
        StringBuilder textoConcatenado = new StringBuilder();
        for (String p : arr) {

            textoConcatenado.append(p).append(" ");
        }
        byte[] textoBytes = textoConcatenado.toString().trim().getBytes(StandardCharsets.UTF_8);
        return  textoBytes;
    }

    static  void startingOpenCL(String texto, String palavra){
        byte[] textoBytes = transformarBytes(texto);
        byte[] palavraBytes = palavra.getBytes(StandardCharsets.UTF_8);
        int textLength = textoBytes.length;
        int wordLength = palavraBytes.length;


        // Array para armazenar o resultado
        int[] count = {0};

        // Inicializar o OpenCL
        CL.setExceptionsEnabled(true);
        cl_platform_id[] platforms = new cl_platform_id[1];
        clGetPlatformIDs(1, platforms, null);
        cl_platform_id platform = platforms[0];

        cl_device_id[] devices = new cl_device_id[1];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, 1, devices, null);
        cl_device_id device = devices[0];

        cl_context context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue queue = clCreateCommandQueueWithProperties(context, device, null, null);

        // Criar buffers para texto, palavra e contador
        cl_mem textBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_char * textLength, Pointer.to(textoBytes), null);

        cl_mem wordBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_char * wordLength, Pointer.to(palavraBytes), null);

        cl_mem countBuffer = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_int, Pointer.to(count), null);

        // Compilar o kernel
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{KERNEL_CODE}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel kernel = clCreateKernel(program, "count_word", null);

        // Setar argumentos do kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(textBuffer));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(wordBuffer));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{wordLength}));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{textLength}));
        clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(countBuffer));

        // Definir o número de threads
        long[] globalWorkSize = new long[]{textLength};

        // Executar o kernel
        clEnqueueNDRangeKernel(queue, kernel, 1, null, globalWorkSize, null, 0, null, null);

        // Ler o resultado do buffer
        clEnqueueReadBuffer(queue, countBuffer, CL_TRUE, 0, Sizeof.cl_int, Pointer.to(count), 0, null, null);

        // Exibir o resultado
        System.out.println("A palavra \"" + palavra + "\" aparece " + count[0] + " vezes no texto.");

        // Limpar recursos
        clReleaseMemObject(textBuffer);
        clReleaseMemObject(wordBuffer);
        clReleaseMemObject(countBuffer);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(queue);
        clReleaseContext(context);

    }


}
