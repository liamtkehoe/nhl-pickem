import { DndContext, KeyboardSensor, PointerSensor, closestCenter, useSensor, useSensors, type DragEndEvent } from '@dnd-kit/core'
import { SortableContext, arrayMove, sortableKeyboardCoordinates, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import type { Dispatch, SetStateAction } from 'react'
import type { DivisionOption, OverUnder, TeamOption } from '../api'
import type { Draft } from './useDraft'
import { TeamLogo } from '../images'

interface Props {
    division: DivisionOption
    draft: Draft
    setDraft: Dispatch<SetStateAction<Draft>>
}

export function DivisionStep({ division, draft, setDraft }: Props) {
    const sensors = useSensors(
        useSensor(PointerSensor),
        useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
    )
    const order = draft.order[division.division] ?? []
    const teamsByCode = new Map(division.teams.map(t => [t.team, t]))

    function handleDragEnd(event: DragEndEvent) {
        const { active, over } = event
        if (!over || active.id === over.id) return
        const from = order.indexOf(String(active.id))
        const to = order.indexOf(String(over.id))
        setDraft(d => ({
            ...d,
            order: { ...d.order, [division.division]: arrayMove(order, from, to) },
        }))
    }

    function setPick(team: string, pick: OverUnder) {
        setDraft(d => ({ ...d, overUnders: { ...d.overUnders, [team]: pick } }))
    }

    return (
        <section>
            <h2>{division.division} Division</h2>
            <p className="hint">
                Drag teams into the order you think they'll finish, 1st at the top. Then pick over or under
                on each team's season points line.
            </p>
            <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                <SortableContext items={order} strategy={verticalListSortingStrategy}>
                    <ol className="ranking">
                        {order.map((code, index) => {
                            const team = teamsByCode.get(code)
                            if (!team) return null
                            return (
                                <TeamRow
                                    key={code}
                                    place={index + 1}
                                    team={team}
                                    pick={draft.overUnders[code]}
                                    onPick={pick => setPick(code, pick)}
                                />
                            )
                        })}
                    </ol>
                </SortableContext>
            </DndContext>
        </section>
    )
}

interface RowProps {
    place: number
    team: TeamOption
    pick: OverUnder | undefined
    onPick: (pick: OverUnder) => void
}

function TeamRow({ place, team, pick, onPick }: RowProps) {
    const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
        useSortable({ id: team.team })
    const style = { transform: CSS.Transform.toString(transform), transition }

    return (
        <li ref={setNodeRef} style={style} className={isDragging ? 'team-row dragging' : 'team-row'}>
      <span className="handle" {...attributes} {...listeners} aria-label={`Move ${team.teamName}`}>
        ⠿
      </span>
            <span className="place">{place}</span>
            <TeamLogo team={team.team} />
            <span className="name">{team.teamName}</span>
            <span className="line">{team.line ?? '–'}</span>
            <span className="ou">
        {(['OVER', 'UNDER'] as const).map(side => (
            <button
                key={side}
                type="button"
                className={pick === side ? 'selected' : ''}
                onClick={() => onPick(side)}
            >
                {side === 'OVER' ? 'Over' : 'Under'}
            </button>
        ))}
      </span>
        </li>
    )
}