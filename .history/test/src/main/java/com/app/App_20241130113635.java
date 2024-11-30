package com.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.jocl.CL;
import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
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
    "__kernel void count_word(__global const char *text, __global const char *word, __global int *results, const int wordLength, const int totalWords) {" +
    "    int id = get_global_id(0);" +
    "    int matches = 1;" +
    "    for (int i = 0; i < wordLength; i++) {" +
    "        if (text[id * wordLength + i] != word[i]) {" +
    "            matches = 0;" +
    "            break;" +
    "        }" +
    "    }" +
    "    results[id] = matches;" +
    "}";
    public static void main( String[] args )
    {
        // System.out.println( "Hello World!" );
        String texto = readFile("test/src/main/java/com/files/carlosText.txt");
        // String[] arr = stringToArray(texto);
        // for (String word : arr) {
        //     System.out.println(word);
            
        // }
        // long inicio = System.nanoTime();
        // int qtd = contadorPalavra(arr, "carlos");
        // long fim = System.nanoTime();

        // System.out.println(qtd);
        // System.out.println(fim-inicio);
        // System.out.println(texto);
        startingOpenCL(texto, "carlos");
        
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
        int totalPalavras = stringToArray(texto).length;
        int palavraTamanho = palavra.length();


         // Configuração do OpenCL
        CL.setExceptionsEnabled(true);
        cl_platform_id[] platforms = new cl_platform_id[1];
        clGetPlatformIDs(1, platforms, null);
        cl_device_id[] devices = new cl_device_id[1];
        clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_GPU, 1, devices, null);
        cl_context context = clCreateContext(null, 1, devices, null, null, null);
        cl_command_queue queue = clCreateCommandQueueWithProperties(context, devices[0], null, null);

         // Buffers de entrada e saída
        cl_mem textBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, 
                                           Sizeof.cl_char * textoBytes.length, Pointer.to(textoBytes), null);
        cl_mem wordBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, 
                                           Sizeof.cl_char * palavraBytes.length, Pointer.to(palavraBytes), null);
        cl_mem resultBuffer = clCreateBuffer(context, CL_MEM_WRITE_ONLY, 
                                             Sizeof.cl_int * totalPalavras, null, null);


        // Compilar o kernel
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{KERNEL_CODE}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel kernel = clCreateKernel(program, "count_word", null);

        // Configurar os argumentos do kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(textBuffer));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(wordBuffer));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(resultBuffer));
        clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{palavraTamanho}));
        clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{totalPalavras}));



                // Configurar e executar o kernel
        long globalWorkSize[] = new long[]{totalPalavras};
        clEnqueueNDRangeKernel(queue, kernel, 1, null, globalWorkSize, null, 0, null, null);

        // Ler os resultados
        int[] results = new int[totalPalavras];
        clEnqueueReadBuffer(queue, resultBuffer, CL_TRUE, 0, results.length * Sizeof.cl_int, Pointer.to(results), 0, null, null);

        // Contar as ocorrências
        int total = 0;
        for (int result : results) {
            total += result;
        }

        
        System.out.println("A palavra \"" + palavra + "\" aparece " + total + " vezes no texto.");

        // Limpar os recursos
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseMemObject(textBuffer);
        clReleaseMemObject(wordBuffer);
        clReleaseMemObject(resultBuffer);
        clReleaseCommandQueue(queue);
        clReleaseContext(context);

    }


}
