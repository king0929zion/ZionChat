import 'package:flutter/material.dart';

import '../models/chat_models.dart';

class MessageBubble extends StatelessWidget {
  const MessageBubble({
    super.key,
    required this.message,
    required this.onCopy,
    required this.onActionTap,
  });

  final ChatMessage message;
  final VoidCallback onCopy;
  final void Function(String label) onActionTap;

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.sizeOf(context).width;
    if (message.isUser) {
      return Align(
        alignment: Alignment.centerRight,
        child: ConstrainedBox(
          constraints: BoxConstraints(maxWidth: width * 0.65),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            decoration: BoxDecoration(
              color: const Color(0xFFF2F2F7),
              borderRadius: BorderRadius.circular(18),
            ),
            child: Text(
              message.text,
              style: const TextStyle(
                fontSize: 16,
                height: 1.45,
                color: Color(0xFF1C1C1E),
              ),
            ),
          ),
        ),
      );
    }

    return Align(
      alignment: Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: width * 0.85),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              message.text,
              style: const TextStyle(
                fontSize: 16,
                height: 1.45,
                color: Color(0xFF1C1C1E),
              ),
            ),
            const SizedBox(height: 6),
            Wrap(
              spacing: 2,
              children: [
                _ActionButton(icon: Icons.copy_all_outlined, onTap: onCopy),
                _ActionButton(
                  icon: Icons.edit_outlined,
                  onTap: () => onActionTap('编辑'),
                ),
                _ActionButton(
                  icon: Icons.volume_up_outlined,
                  onTap: () => onActionTap('语音'),
                ),
                _ActionButton(
                  icon: Icons.share_outlined,
                  onTap: () => onActionTap('分享'),
                ),
                _ActionButton(
                  icon: Icons.refresh_rounded,
                  onTap: () => onActionTap('重试'),
                ),
                _ActionButton(
                  icon: Icons.more_horiz_rounded,
                  onTap: () => onActionTap('更多'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class SelectedToolChip extends StatelessWidget {
  const SelectedToolChip({
    super.key,
    required this.tool,
    required this.onClear,
  });

  final ToolOption tool;
  final VoidCallback onClear;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFFE8F4FD),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(tool.icon, size: 18, color: const Color(0xFF007AFF)),
          const SizedBox(width: 8),
          Text(
            tool.label,
            style: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w600,
              color: Color(0xFF007AFF),
            ),
          ),
          const SizedBox(width: 6),
          GestureDetector(
            onTap: onClear,
            child: Container(
              width: 18,
              height: 18,
              decoration: const BoxDecoration(
                color: Color(0xFFD1E8FA),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.close_rounded,
                size: 14,
                color: Color(0xFF007AFF),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionButton extends StatelessWidget {
  const _ActionButton({required this.icon, required this.onTap});

  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Icon(icon, size: 18, color: const Color(0xFF374151)),
        ),
      ),
    );
  }
}
