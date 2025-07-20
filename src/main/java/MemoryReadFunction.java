import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

import java.time.Duration;
import java.util.HashMap;

public class MemoryReadFunction implements Runnable {
    // dll (kernel32)
    public interface MyKernel32 extends Kernel32 {
        MyKernel32 INSTANCE = (MyKernel32) Native.load("kernel32", MyKernel32.class);

        HANDLE OpenProcess(int i, boolean b, int i1);
        boolean ReadProcessMemory(HANDLE handle, Pointer pointer, Pointer pointer1, int i, IntByReference intByReference);
        boolean CloseHandle(HANDLE handle);
    }

    public interface psapi extends Psapi {
        boolean EnumProcessModules(WinNT.HANDLE handle, WinDef.HMODULE[] hmodules, int i, IntByReference intByReference);
        int GetModuleFileNameExW(WinNT.HANDLE handle, WinNT.HANDLE handle1, char[] chars, int i);
        int GetModuleFileNameExA(WinNT.HANDLE handle, WinNT.HANDLE handle1, byte[] bytes, int i);
    }

    // Function to get the base address of a module (EXE or DLL)
    public long getModuleBaseAddress(int pid, String moduleName) {
        HANDLE hProcess = null;
        try {
            hProcess = MyKernel32.INSTANCE.OpenProcess(
                    WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ, // Needed for EnumProcessModules
                    false,
                    pid
            );

            if (hProcess == null) {
                System.err.println("Failed to open process for module enum. Error: " + Kernel32.INSTANCE.GetLastError());
                return 0;
            }

            WinDef.HMODULE[] hModules = new WinDef.HMODULE[1024]; // Max 1024 modules (adjust as needed)
            IntByReference lpcbNeeded = new IntByReference(0);

            if (!psapi.INSTANCE.EnumProcessModules(hProcess, hModules, new WinDef.DWORD((long) hModules.length * Native.POINTER_SIZE).intValue(), lpcbNeeded)) {
                System.err.println("Failed to enumerate process modules. Error: " + Kernel32.INSTANCE.GetLastError());
                return 0;
            }

            int moduleCount = lpcbNeeded.getValue() / Native.POINTER_SIZE;

            for (int i = 0; i < moduleCount; i++) {
                char[] lpBaseName = new char[256];
                int result = psapi.INSTANCE.GetModuleFileNameExW(hProcess, hModules[i], lpBaseName, new WinDef.DWORD((long) hModules.length * Native.POINTER_SIZE).intValue());
                if (result != 0) {
                    String currentModuleName = new String(lpBaseName).trim();
                    if (currentModuleName.equalsIgnoreCase(moduleName)) {
                        // Get the base address directly from the HMODULE pointer
                        return Pointer.nativeValue(hModules[i].getPointer());
                    }
                } else {
                    MainFunction.classLogger.warn(String.valueOf(MyKernel32.INSTANCE.GetLastError()));
                }
            }
        } catch (Exception e) {
            MainFunction.classLogger.warn(e.getMessage());
        } finally {
            if (hProcess != null) {
                MyKernel32.INSTANCE.CloseHandle(hProcess);
            }
        }
        return 0; // Module not found
    }


    public void RunFunction() {
        int PID = Math.toIntExact(MainFunction.classProcess.pid());

        // hi hashmap
        HashMap<Long, Integer> positionMemory = new HashMap<>();
        positionMemory.put(0x4592C8L, 4);
        positionMemory.put(0x4592CCL, 4);
        positionMemory.put(0x4592D0L, 4);

        HANDLE currentProcessHandle = null;

        try {
            // 2. Open the target process with read access
            currentProcessHandle = MyKernel32.INSTANCE.OpenProcess(
                    WinNT.PROCESS_VM_READ | WinNT.PROCESS_QUERY_INFORMATION, // Needed for reading memory
                    false,
                    PID
            );

            if (currentProcessHandle == null) {
                System.err.println("Fail! Could not open process. Error code: " + Kernel32.INSTANCE.GetLastError());
                return;
            }

            // base address
            long moduleBaseAddress = getModuleBaseAddress(PID, MainFunction.classProcess.info().command().get());
            if (moduleBaseAddress == 0) {
                System.err.println("Failed to find base address for module: " + "NeptuniaReBirth1.exe");
                return;
            }

            // we loop over the hashmap values
            HANDLE finalCurrentProcessHandle = currentProcessHandle;

            positionMemory.forEach((address, size) -> {
                long targetAbsoluteAddress = moduleBaseAddress + address;
                System.out.println("Target Absolute Address: 0x" + Long.toHexString(targetAbsoluteAddress));

                // 5. Read the float value from the calculated address
                int floatSize = 4;
                Memory floatBuffer = new Memory(floatSize);
                IntByReference bytesReadFloat = new IntByReference(0);

                boolean successReadFloat = MyKernel32.INSTANCE.ReadProcessMemory(
                        finalCurrentProcessHandle,
                        new Pointer(targetAbsoluteAddress),
                        floatBuffer,
                        new BaseTSD.SIZE_T(floatSize).intValue(),
                        bytesReadFloat
                );

                if (successReadFloat && bytesReadFloat.getValue() == floatSize) {
                    float currentValue = floatBuffer.getFloat(0);

                    MainFunction.classLogger.warn(String.valueOf(address));
                    if (address.equals(0x4592C8L)) {
                        detailsRecords.Ycoord = currentValue;
                    } else if (address.equals(0x4592CCL)) {
                        detailsRecords.Zcoord = currentValue;
                    } else if (address.equals(0x4592D0L)) {
                        detailsRecords.Xcoord = currentValue;
                    }

                } else {
                    System.err.println("Failed to read float value. Error code: " + Kernel32.INSTANCE.GetLastError());
                }
            });

        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();

        } finally {
            if (currentProcessHandle != null) {
                MyKernel32.INSTANCE.CloseHandle(currentProcessHandle);
            }

        }
    }

    @Override
    public void run() {
        while (true) {
            RunFunction();
            try {
                Thread.sleep(Duration.ofSeconds(9));
            } catch (InterruptedException e) {
                MainFunction.classLogger.error("Something has broke the code! Ending the execution now.");
                break;
            }
        }
    }
}
