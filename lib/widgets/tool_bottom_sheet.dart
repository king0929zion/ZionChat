import 'package:flutter/material.dart';

import '../models/chat_models.dart';

class ToolBottomSheet extends StatelessWidget {
  const ToolBottomSheet({super.key});

  static const List<ToolOption> _gridTools = [
    ToolOption.camera,
    ToolOption.photos,
    ToolOption.files,
  ];

  static const List<ToolOption> _listTools = [
    ToolOption.web,
    ToolOption.image,
    ToolOption.mcp,
  ];

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 40,
              height: 4,
              margin: const EdgeInsets.only(bottom: 16),
              decoration: BoxDecoration(
                color: const Color(0xFFD1D5DB),
                borderRadius: BorderRadius.circular(999),
              ),
            ),
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _gridTools.length,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                mainAxisExtent: 106,
              ),
              itemBuilder: (context, index) {
                final tool = _gridTools[index];
                return Material(
                  color: const Color(0xFFF9FAFB),
                  borderRadius: BorderRadius.circular(20),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(20),
                    onTap: () => Navigator.of(context).pop(tool),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(tool.icon, size: 28, color: Colors.black),
                        const SizedBox(height: 8),
                        Text(
                          tool.label,
                          style: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: Color(0xFF111827),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 16),
            const Divider(height: 1, color: Color(0xFFE5E7EB)),
            const SizedBox(height: 8),
            Column(
              children: _listTools.map((tool) {
                return Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Material(
                    color: Colors.transparent,
                    borderRadius: BorderRadius.circular(16),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(16),
                      onTap: () => Navigator.of(context).pop(tool),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 12,
                        ),
                        child: Row(
                          children: [
                            SizedBox(
                              width: 36,
                              height: 36,
                              child: Center(
                                child: Icon(
                                  tool.icon,
                                  size: 24,
                                  color: Colors.black,
                                ),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    tool.sheetTitle,
                                    style: const TextStyle(
                                      fontSize: 16,
                                      color: Color(0xFF111827),
                                    ),
                                  ),
                                  const SizedBox(height: 2),
                                  Text(
                                    tool.subtitle,
                                    style: const TextStyle(
                                      fontSize: 13,
                                      color: Color(0xFF6B7280),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ],
        ),
      ),
    );
  }
}
