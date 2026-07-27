import { toBlob } from 'html-to-image'

/**
 * Renders an element to a JPG blob for previewing before the user confirms the export.
 * Nodes marked with data-export-exclude="true" are skipped — used for content
 * that can't be captured on canvas, like the Google Maps iframe.
 */
export async function captureElementAsJpgBlob(element: HTMLElement): Promise<Blob> {
  const blob = await toBlob(element, {
    quality: 0.95,
    backgroundColor: '#f7f5ff',
    pixelRatio: 2,
    filter: (node) => !(node instanceof HTMLElement && node.dataset.exportExclude === 'true'),
  })

  if (!blob) {
    throw new Error('Falha ao gerar a imagem.')
  }

  return blob
}

/** Shares (mobile) or downloads (desktop) a previously captured JPG blob. */
export async function shareOrDownloadJpg(blob: Blob, fileName: string, shareTitle?: string) {
  const file = new File([blob], fileName, { type: 'image/jpeg' })

  if (navigator.canShare?.({ files: [file] })) {
    try {
      await navigator.share({ files: [file], title: shareTitle })
      return
    } catch {
      // Cancelled or unsupported — fall back to a plain download below.
    }
  }

  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
