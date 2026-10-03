import { BrowserWindow, dialog, ipcMain } from 'electron'
import { writeFile } from 'node:fs/promises'

// The renderer cannot reach the disk. It hands main the CSV and a default name;
// main opens the system's save dialog over the window that asked, and writes
// the file where the person chooses. A cancel writes nothing.
export function handleCsvSave() {
  ipcMain.handle('csv:save', async (event, defaultName: string, csv: string) => {
    const window = BrowserWindow.fromWebContents(event.sender)
    if (!window) return
    const { canceled, filePath } = await dialog.showSaveDialog(window, {
      defaultPath: defaultName,
      filters: [{ name: 'CSV', extensions: ['csv'] }],
    })
    if (!canceled && filePath) await writeFile(filePath, csv)
  })
}
