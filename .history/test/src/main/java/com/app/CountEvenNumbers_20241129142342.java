package com.app;
import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

public class CountEvenNumbers {
    private static final String KERNEL_CODE =
            "__kernel void count_evens(__global const int *array, __global int *output) {" +
            "    int id = get_global_id(0);" +
            "    output[id] = (array[id] % 2 == 0) ? 1 : 0;" +
            "}";

    public static void main(String[] args) {
        // Habilitar exceções para erros do OpenCL
        CL.setExceptionsEnabled(true);

        // Dados de entrada
        int[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int arraySize = array.length;

        // Buffer de saída para resultados
        int[] result = new int[arraySize];

        // Etapa 1: Selecionar plataforma e dispositivo
        cl_platform_id[] platforms = new cl_platform_id[1];
        CL.clGetPlatformIDs(1, platforms, null);
        cl_platform_id platform = platforms[0];

        cl_device_id[] devices = new cl_device_id[1];
        CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_GPU, 1, devices, null);
        cl_device_id device = devices[0];

        // Etapa 2: Criar contexto e fila de comandos
        cl_context context = CL.clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        cl_command_queue commandQueue = CL.clCreateCommandQueue(context, device, 0, null);

        // Etapa 3: Criar buffers de memória no dispositivo
        cl_mem bufferArray = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_int * arraySize, Pointer.to(array), null);
        cl_mem bufferResult = CL.clCreateBuffer(context, CL.CL_MEM_WRITE_ONLY,
                Sizeof.cl_int * arraySize, null, null);

        // Etapa 4: Criar e compilar o kernel
        cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{KERNEL_CODE}, null, null);
        CL.clBuildProgram(program, 0, null, null, null, null);
        cl_kernel kernel = CL.clCreateKernel(program, "count_evens", null);

        // Etapa 5: Configurar argumentos do kernel
        CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(bufferArray));
        CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(bufferResult));

        // Etapa 6: Configurar tamanho do trabalho (threads)
        long[] globalWorkSize = new long[]{arraySize}; // Cada elemento do array será processado por uma thread

        // Etapa 7: Executar o kernel
        CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, null, 0, null, null);

        // Etapa 8: Copiar resultados de volta para a CPU
        CL.clEnqueueReadBuffer(commandQueue, bufferResult, CL.CL_TRUE, 0,
                Sizeof.cl_int * arraySize, Pointer.to(result), 0, null, null);

        // Contar o número de pares
        int count = 0;
        for (int value : result) {
            count += value;
        }

        // Exibir resultado
        System.out.println("Número de pares: " + count);

        // Limpar recursos do OpenCL
        CL.clReleaseKernel(kernel);
        CL.clReleaseProgram(program);
        CL.clReleaseMemObject(bufferArray);
        CL.clReleaseMemObject(bufferResult);
        CL.clReleaseCommandQueue(commandQueue);
        CL.clReleaseContext(context);
    }
}
