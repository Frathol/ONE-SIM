from PIL import Image, ImageDraw

img = Image.new('RGB', (600, 600), color='white')
draw = ImageDraw.Draw(img)

line_color = (150, 150, 150)
line_width = 3

draw.line((200, 0, 200, 600), fill=line_color, width=line_width)
draw.line((400, 0, 400, 600), fill=line_color, width=line_width)

draw.line((0, 200, 600, 200), fill=line_color, width=line_width)
draw.line((0, 400, 600, 400), fill=line_color, width=line_width)

img.save('3x3Grid.png')