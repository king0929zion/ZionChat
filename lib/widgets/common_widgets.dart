import 'package:flutter/material.dart';

class RoundSurfaceButton extends StatelessWidget {
  const RoundSurfaceButton({
    super.key,
    required this.size,
    required this.child,
    required this.onTap,
    this.backgroundColor = Colors.white,
    this.margin = EdgeInsets.zero,
  });

  final double size;
  final Widget child;
  final VoidCallback onTap;
  final Color backgroundColor;
  final EdgeInsetsGeometry margin;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      margin: margin,
      decoration: BoxDecoration(color: backgroundColor, shape: BoxShape.circle),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          customBorder: const CircleBorder(),
          onTap: onTap,
          child: Center(child: child),
        ),
      ),
    );
  }
}

class HamburgerIcon extends StatelessWidget {
  const HamburgerIcon({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: 20,
          height: 2,
          decoration: BoxDecoration(
            color: const Color(0xFF1C1C1E),
            borderRadius: BorderRadius.circular(99),
          ),
        ),
        const SizedBox(height: 5),
        Align(
          alignment: Alignment.centerLeft,
          child: Container(
            width: 12,
            height: 2,
            margin: const EdgeInsets.only(left: 11),
            decoration: BoxDecoration(
              color: const Color(0xFF1C1C1E),
              borderRadius: BorderRadius.circular(99),
            ),
          ),
        ),
      ],
    );
  }
}

class MessageEntrance extends StatelessWidget {
  const MessageEntrance({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      duration: const Duration(milliseconds: 260),
      curve: Curves.easeOutCubic,
      tween: Tween(begin: 0, end: 1),
      child: child,
      builder: (context, value, child) {
        return Opacity(
          opacity: value,
          child: Transform.translate(
            offset: Offset(0, (1 - value) * 10),
            child: child,
          ),
        );
      },
    );
  }
}
