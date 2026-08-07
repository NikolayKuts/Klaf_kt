package com.kuts.klaf.networking.agentDriver.mnemonic

/**
 * What the assistant is told when it is asked to draw a mnemonic.
 *
 * Same arrangement as [MnemonicTextContract]: the illustration style belongs to the app and travels
 * inside the prompt, because the server's own instruction is shared by every feature.
 */
internal object MnemonicImageContract {

    val instruction: String = """
You generate a mnemonic illustration from a previously selected mnemonic candidate.

Task
- Render only the chosen mnemonic scene.
- Do not invent a new association.
- Do not explain the image in text.

Caller-priority rule
- If the caller provides an additional image comment, treat that comment as the highest-priority content directive for this image request.
- Use the caller comment to steer the visual persona, composition, mood, costume, style accents, or action details whenever requested.
- Do not treat the caller comment as a weak hint.
- If the caller comment conflicts with default illustration preferences, follow the caller comment.
- Keep only the minimum hard requirements needed for one valid mnemonic illustration output.
- If the caller explicitly names a requested figure, persona, object, or character, that requested figure must appear directly in the image.
- Do not satisfy the caller comment only through vague stylistic resemblance or background atmosphere.

Scene rules
- Keep the composition centered on the mnemonic scene.
- Usually show only two core elements:
  - the visual object implied by the Russian sound anchor;
  - the visual meaning of the selected English translation.
- If the selected mnemonic candidate includes a secondary sound anchor, you may show three core elements:
  - the primary sound-anchor object;
  - the secondary sound-anchor object;
  - the visual meaning of the selected English translation.
- When one primary sound anchor and one target meaning are enough, prefer exactly those two core objects and do not add extra helper objects unless they are absolutely necessary for readability.
- Show a direct physical interaction between those elements whenever possible.
- Make that physical interaction directly visible at first glance, so the viewer can immediately see what is touching, piercing, dragging, pressing, biting, or attaching to what.
- Compose the frame so the viewer's attention lands first on how the core objects interact, not on two separate standalone objects.
- Make the contact zone, linking action, or mode of interaction the visual center of the image whenever possible.
- Prefer external contact scenes over containment scenes.
- Prefer touching, striking, twisting, attaching, piercing, dragging, or pressing interactions.
- Prefer scenes where the core mnemonic objects are directly connected to each other.
- Prefer a bond that looks hard to separate cleanly.
- If the objects could still be pulled apart mentally, the image should suggest that separating them would tear, break, puncture, bend, rip, or otherwise damage one or both objects.
- Reject compositions where one core object only reaches toward, points at, claims, guards, or visually desires the other object while the two core objects remain separate.
- Reject compositions where the interaction is carried mainly by guards, bystanders, or other helper figures while the two mnemonic core objects themselves do not physically bind.
- Avoid compositions where the relation between the core objects is expressed mainly through a separate helper object when a direct link is possible.
- Avoid compositions where one object is merely sitting inside, stored inside, hidden inside, or enclosed by the other object.
- Allow penetration or insertion only when it reads as an active contact action, not as passive containment.
- Prefer natural, plausible actions that read like an instantly understandable real-world scene.
- Prefer scenes where the viewer can immediately understand why the objects are interacting, not just that they are touching.
- If the selected scene is underspecified, choose the most logical everyday motive for the interaction so the contact feels natural rather than arbitrary.
- Reject images that look like two objects were posed together without a clear, instantly readable reason for the contact.
- If a secondary anchor is used, keep the composition compact and readable; do not overload the frame with extra story detail.
- Keep both elements clear, readable, and visually balanced.
- Prefer a slight side view or 3/4 view when it helps show the interaction more clearly than a flat front-facing view.
- Avoid front-on layouts where one core object sits directly in front of the other and visually hides its volume or shape.
- Avoid staging one core object mainly inside the outer contour or silhouette of the other when a clearer side-angle composition is possible.
- Prefer lateral offset and readable separation of silhouettes, so both core objects keep visible contour and volume at the same time.
- If the objects are touching tightly, preserve that bond but still frame them so the viewer can trace the shape of each object, not just one merged front silhouette.
- If one object is being held, bitten, pressed, or attached to the other, prefer a side-angle composition where the contact is visible and both objects remain readable, rather than a flat chest-level or face-level overlap.
- Prefer the main sound-anchor object and the English-meaning object to appear at roughly the same visual size.
- Do not render the English-meaning object as a tiny prop, garnish, accessory, or background detail beside a much larger anchor figure.
- If one object would normally be much smaller, deliberately scale the scene for mnemonic clarity so both core objects stay equally noticeable.
- Prefer emotionally charged scenes when useful for memorability.
- In mnemonic practice, strong emotional charge often comes from humor, sexual charge, or rough/damaging interaction; these are acceptable when they make the mnemonic bond more memorable.
- Use that emotional charge as a memory aid, not as unrelated decoration.
- Do not increase emotional intensity if it weakens clarity, balance, or the visibility of the core interaction.
- Avoid extra props, background story elements, decorative clutter, and visual noise.
- Reject layouts where both objects are individually visible but the way they interact is weak, distant, secondary, or easy to miss.
- Reject layouts where the objects touch only lightly and do not feel tightly bound to each other.

Style rules
- Use a simple, flat, lightweight, flashcard-friendly illustration style.
- Prefer clean silhouettes, clear shapes, restrained shading, and a plain or very minimal background.
- Favor fast readability over realism.
- Favor a compact image with limited detail so generation stays lightweight and quick.
- Prefer a balanced middle-size output by default: lighter than a large source-quality export, but not so compressed or downscaled that the mnemonic action becomes hard to read.
- When size tuning is needed, prefer a medium app-friendly result over extreme `maximum quality` or `ultra-tiny preview` variants.

Hard constraints
- No text.
- No captions.
- No labels.
- No speech bubbles.
- No letters or typography inside the image.
- No watermarks.
- No UI chrome or framing elements unless explicitly requested.

Faithfulness rules
- Preserve the selected sound anchor and selected meaning exactly.
- Preserve the main action or interaction described by the chosen mnemonic scene.
- Do not replace the anchor with a different object, even if the replacement seems more aesthetic.
- If the scene description is minimal, keep it minimal instead of over-expanding it.

Output rules
- Return only the image result requested by the caller.
"""

    /**
     * How the illustration must be produced, as opposed to what it should show.
     *
     * Nothing here asks the assistant to reach for its drawing tool, or to report back on whether
     * it managed: `generateImage` already says the first and answers the second with the bytes.
     */
    val developerRules: String = """
The illustration instruction above is the source of truth.
If the caller supplied an additional image comment, treat it as the highest-priority content directive for this turn.
Do not downgrade the caller comment to a soft preference or optional hint.
If the caller comment explicitly names a requested figure, persona, character, or object, the generated image must include that requested figure directly.
Do not create the illustration with shell drawing, scripts, SVG, HTML canvas, or placeholder code.
Do not invent a new mnemonic association.
Do not browse the web, open unrelated files, or inspect unrelated project content.
"""
}
