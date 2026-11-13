export interface TreeNode {
  id: any
  parentId?: any
  children?: TreeNode[]
  [key: string]: any
}

/**
 * 平铺列表转树（parentId 缺省视为 0，父节点不存在时也归入根）。
 */
export function buildTree<T extends TreeNode>(list: T[], rootParentId: any = 0): TreeNode[] {
  const nodeMap = new Map<any, TreeNode>()
  list.forEach(item => nodeMap.set(item.id, { ...item, children: [] }))
  const roots: TreeNode[] = []
  list.forEach(item => {
    const node = nodeMap.get(item.id)!
    const parentId = item.parentId ?? 0
    const parent = nodeMap.get(parentId)
    if (parentId === rootParentId || !parent) {
      roots.push(node)
    } else {
      parent.children!.push(node)
    }
  })
  return roots
}
