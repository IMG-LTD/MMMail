import { describe, expect, it } from 'vitest'
import type { MailFolderNode } from '../types/mail-folders'
import {
  buildMailFolderPayload,
  createMailFolderDraft,
  findMailFolderNode,
  findMailFolderPath,
  flattenMailFolderTree
} from '../utils/mail-folders'

const FOLDER_TREE: MailFolderNode[] = [
  {
    id: 'root-1',
    parentId: null,
    name: 'Projects',
    color: '#5B7CFA',
    notificationsEnabled: true,
    unreadCount: 4,
    totalCount: 12,
    updatedAt: '2026-03-10T07:00:00',
    children: [
      {
        id: 'child-1',
        parentId: 'root-1',
        name: 'Launch',
        color: '#0F6E6E',
        notificationsEnabled: false,
        unreadCount: 2,
        totalCount: 6,
        updatedAt: '2026-03-10T07:05:00',
        children: []
      }
    ]
  }
]

describe('mail folders utils', () => {
  it('builds a normalized folder payload', () => {
    const draft = createMailFolderDraft('root-1')
    draft.name = ' Launch '
    draft.color = ' #0f6e6e '
    draft.notificationsEnabled = false

    expect(buildMailFolderPayload(draft)).toEqual({
      name: 'Launch',
      color: '#0F6E6E',
      parentId: 'root-1',
      notificationsEnabled: false
    })
  })

  it('flattens the folder tree with depth labels', () => {
    expect(flattenMailFolderTree(FOLDER_TREE)).toEqual([
      {
        id: 'root-1',
        name: 'Projects',
        label: 'Projects',
        color: '#5B7CFA',
        depth: 0,
        parentId: null,
        unreadCount: 4,
        totalCount: 12
      },
      {
        id: 'child-1',
        name: 'Launch',
        label: '· Launch',
        color: '#0F6E6E',
        depth: 1,
        parentId: 'root-1',
        unreadCount: 2,
        totalCount: 6
      }
    ])
  })

  it('finds folders and resolves breadcrumb path', () => {
    expect(findMailFolderNode(FOLDER_TREE, 'child-1')?.name).toBe('Launch')
    expect(findMailFolderPath(FOLDER_TREE, 'child-1').map((item) => item.name)).toEqual(['Projects', 'Launch'])
    expect(findMailFolderPath(FOLDER_TREE, 'missing')).toEqual([])
  })
})
